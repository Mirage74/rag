package com.balex.rag.service.impl;

import com.balex.rag.model.LoadedDocument;
import com.balex.rag.model.UploadProgress;
import com.balex.rag.model.constants.ApiLogMessage;
import com.balex.rag.model.exception.UploadException;
import com.balex.rag.model.response.RagResponse;
import com.balex.rag.repo.DocumentRepository;
import com.balex.rag.service.UserDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static com.balex.rag.model.constants.ApiConstants.*;
import static com.balex.rag.model.constants.ApiErrorMessage.UPLOADED_FILENAME_EMPTY;
import static com.balex.rag.model.constants.ApiErrorMessage.UPLOAD_FILE_READ_ERROR;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDocumentServiceImpl implements UserDocumentService {

    private final DocumentRepository documentRepository;
    private final VectorStore vectorStore;
    private final DocumentTransactionalHelper transactionalHelper;

    private static final String TXT_EXTENSION = "txt";
    private static final String USER_ID_FIELD_NAME = "user_id";

    private static final String STATUS_PROCESSING = "processing";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_ERROR = "error";
    private static final String STATUS_SKIPPED = "skipped";

    @Value("${app.document.chunk-size:200}")
    private int chunkSize;

    @Override
    @Transactional
    public RagResponse<String> processUploadedFiles(List<MultipartFile> files, Long userId) {
        List<String> processedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            String filename = getFilename(file);
            boolean processed = processFileInternal(file, userId, filename);
            if (processed) {
                processedFiles.add(filename);
            }
        }

        String message = processedFiles.isEmpty()
                ? NO_NEW_DOCUMENTS_UPLOADED
                : DOCUMENTS_UPLOADED + String.join(", ", processedFiles);

        return RagResponse.createSuccessful(message);
    }

    @Override
    public Flux<UploadProgress> processUploadedFilesWithProgress(List<MultipartFile> files, Long userId) {
        List<MultipartFile> validFiles = files.stream()
                .filter(file -> !file.isEmpty())
                .toList();

        if (validFiles.isEmpty()) {
            return Flux.just(UploadProgress.builder()
                    .percent(100)
                    .processedFiles(0)
                    .totalFiles(0)
                    .currentFile("")
                    .status(STATUS_COMPLETED)
                    .build());
        }

        return Flux.<UploadProgress>create(sink ->
                        processFilesWithSink(validFiles, userId, sink), FluxSink.OverflowStrategy.BUFFER)
                .subscribeOn(Schedulers.boundedElastic());
    }

    private void processFilesWithSink(List<MultipartFile> validFiles, Long userId, FluxSink<UploadProgress> sink) {
        int totalFiles = validFiles.size();
        int processedCount = 0;

        try {
            for (MultipartFile file : validFiles) {
                String filename = getFilename(file);

                emitProgress(sink, processedCount, totalFiles, filename, STATUS_PROCESSING);

                try {
                    // Call through separate bean to get proper @Transactional proxy
                    boolean processed = transactionalHelper.processFileInTransaction(
                            file, userId, filename, this::processFileInternal);

                    processedCount++;
                    String status = processed ? STATUS_PROCESSING : STATUS_SKIPPED;
                    emitProgress(sink, processedCount, totalFiles, filename, status);

                } catch (UploadException e) {
                    log.error("Error processing file: {}", filename, e);
                    emitProgress(sink, processedCount, totalFiles, filename, STATUS_ERROR);
                    processedCount++;
                }
            }

            sink.next(UploadProgress.builder()
                    .percent(100)
                    .processedFiles(processedCount)
                    .totalFiles(totalFiles)
                    .currentFile("")
                    .status(STATUS_COMPLETED)
                    .build());

            sink.complete();

        } catch (Exception e) {
            log.error("Unexpected error during file processing", e);
            sink.error(e);
        }
    }

    /**
     * Core file processing logic - package-private for transactional helper access.
     *
     * @return true if a file was processed, false if skipped (duplicate)
     */
    boolean processFileInternal(MultipartFile file, Long userId, String filename) {
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new UploadException(UPLOAD_FILE_READ_ERROR + filename, e);
        }

        String contentHash = computeSha256Hash(content);

        if (documentRepository.existsByFilenameAndContentHash(filename, contentHash)) {
            log.debug("Skipping duplicate file: {} with hash: {}", filename, contentHash);
            return false;
        }

        processTextAndStore(userId, filename, content, contentHash);
        return true;
    }

    private void processTextAndStore(Long userId, String filename, byte[] content, String contentHash) {
        Resource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        List<Document> docs = new TextReader(resource).get();

        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(chunkSize)
                .build();
        List<Document> chunks = splitter.apply(docs);

        for (Document chunk : chunks) {
            chunk.getMetadata().put(USER_ID_FIELD_NAME, userId);
        }

        storeInVectorDb(chunks);

        LoadedDocument loaded = LoadedDocument.builder()
                .documentType(getExtensionOrTxt(filename))
                .chunkCount(chunks.size())
                .filename(filename)
                .contentHash(contentHash)
                .userId(userId)
                .build();

        documentRepository.save(loaded);

        log.info("Successfully processed file: {} with {} chunks for user: {}",
                filename, chunks.size(), userId);
    }

    /**
     * Stores documents in vector store with retry via Spring Retry.
     */
    @Retryable(
            retryFor = RuntimeException.class,
            maxAttemptsExpression = "${app.document.retry.max-attempts:3}",
            backoff = @Backoff(
                    delayExpression = "${app.document.retry.delay-ms:1500}",
                    multiplier = 2
            )
    )
    public void storeInVectorDb(List<Document> chunks) {
        vectorStore.accept(chunks);
    }

    private String getFilename(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), UPLOADED_FILENAME_EMPTY);
            return EMPTY_FILENAME;
        }
        return filename;
    }

    private String getExtensionOrTxt(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx == -1 || idx == filename.length() - 1) {
            return TXT_EXTENSION;
        }
        return filename.substring(idx + 1).toLowerCase();
    }

    private String computeSha256Hash(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private void emitProgress(FluxSink<UploadProgress> sink, int processedCount,
                              int totalFiles, String filename, String status) {
        int percent = totalFiles > 0
                ? (int) Math.round((double) processedCount / totalFiles * 100)
                : 0;

        sink.next(UploadProgress.builder()
                .percent(percent)
                .processedFiles(processedCount)
                .totalFiles(totalFiles)
                .currentFile(filename)
                .status(status)
                .build());
    }
}