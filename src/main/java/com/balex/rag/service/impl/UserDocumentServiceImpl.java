package com.balex.rag.service.impl;

import com.balex.rag.model.LoadedDocument;
import com.balex.rag.model.UploadProgress;
import com.balex.rag.model.constants.ApiLogMessage;
import com.balex.rag.model.exception.UploadException;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.balex.rag.model.constants.ApiConstants.EMPTY_FILENAME;
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
    private static final String STATUS_SKIPPED = "skipped";
    private static final Long SSE_EMITTER_TIMEOUT_IN_MILLIS = 120000L;

    @Value("${app.document.chunk-size:200}")
    private int chunkSize;

    public SseEmitter processUploadedFilesWithSse(List<MultipartFile> files, Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_EMITTER_TIMEOUT_IN_MILLIS);

        AtomicBoolean isCompleted = new AtomicBoolean(false);

        emitter.onCompletion(() -> {
            log.debug("SSE completed");
            isCompleted.set(true);
        });
        emitter.onTimeout(() -> {
            log.debug("SSE timeout");
            isCompleted.set(true);
        });
        emitter.onError(e -> {
            // Не логируем как ошибку - клиент просто отключился
            log.debug("SSE client disconnected: {}", e.getMessage());
            isCompleted.set(true);
        });

        List<MultipartFile> validFiles = files.stream()
                .filter(f -> !f.isEmpty())
                .toList();

        CompletableFuture.runAsync(() -> {
            try {
                int totalFiles = validFiles.size();
                int processedCount = 0;

                for (MultipartFile file : validFiles) {
                    if (isCompleted.get()) {
                        log.debug("Upload cancelled, stopping at file: {}", processedCount);
                        return;  // Просто выходим, не бросаем исключение
                    }

                    String filename = getFilename(file);

                    sendProgress(emitter, isCompleted, processedCount, totalFiles, filename, STATUS_PROCESSING);

                    if (isCompleted.get()) return;  // Проверка после отправки

                    boolean processed = transactionalHelper.processFileInTransaction(
                            file, userId, filename, this::processFileInternal);

                    processedCount++;
                    String status = processed ? STATUS_PROCESSING : STATUS_SKIPPED;
                    sendProgress(emitter, isCompleted, processedCount, totalFiles, filename, status);
                }

                if (!isCompleted.get()) {
                    try {
                        emitter.send(SseEmitter.event()
                                .data(UploadProgress.builder()
                                        .percent(100)
                                        .processedFiles(processedCount)
                                        .totalFiles(totalFiles)
                                        .currentFile("")
                                        .status(STATUS_COMPLETED)
                                        .build()));
                        emitter.complete();
                    } catch (IOException | IllegalStateException e) {
                        log.debug("Could not send completion: {}", e.getMessage());
                    }
                }

            } catch (Exception e) {
                if (!isCompleted.get()) {
                    log.error("SSE processing error", e);
                    emitter.completeWithError(e);
                }
            }
        });

        return emitter;
    }

    private void sendProgress(SseEmitter emitter, AtomicBoolean isCompleted,
                              int processed, int total, String filename, String status) {
        if (isCompleted.get()) {
            return;
        }

        try {
            int percent = total > 0 ? (int) Math.round((double) processed / total * 100) : 0;

            emitter.send(SseEmitter.event()
                    .data(UploadProgress.builder()
                            .percent(percent)
                            .processedFiles(processed)
                            .totalFiles(total)
                            .currentFile(filename)
                            .status(status)
                            .build()));
        } catch (IOException | IllegalStateException e) {
            // Client disconnected - this is normal for cancel
            log.debug("Client disconnected: {}", e.getMessage());
            isCompleted.set(true);
        }
    }
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

}