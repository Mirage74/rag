package com.balex.rag.service.impl;

import com.balex.rag.model.LoadedDocument;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
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
    private final static String TXT_EXTENSION = "txt";
    private final static String USER_ID_FIELD_NAME = "user_id";

    @Override
    @Transactional
    public RagResponse<String> processUploadedFiles(List<MultipartFile> files, Long userId) {
        List<String> processedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            String filename = file.getOriginalFilename();
            if (filename == null) {
                filename = EMPTY_FILENAME;
                log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), UPLOADED_FILENAME_EMPTY);
            }
            final String finalFilename = filename;

            byte[] content;
            try {
                content = file.getBytes();
            } catch (IOException e) {
                throw new UploadException(UPLOAD_FILE_READ_ERROR + finalFilename + e);
            }

            String contentHash = DigestUtils.md5DigestAsHex(content);

            if (documentRepository.existsByFilenameAndContentHash(finalFilename, contentHash)) {
                continue;
            }

            Resource resource = new ByteArrayResource(content) {
                @Override
                public String getFilename() {
                    return finalFilename;
                }
            };

            List<Document> docs = new TextReader(resource).get();
            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(200)
                    .build();
            List<Document> chunks = splitter.apply(docs);

            for (Document c : chunks) {
                c.getMetadata().put(USER_ID_FIELD_NAME, userId);
            }

            acceptWithRetry(vectorStore, chunks, 3, 1500);

            LoadedDocument loaded = LoadedDocument.builder()
                    .documentType(getExtensionOrTxt(finalFilename))
                    .chunkCount(chunks.size())
                    .filename(finalFilename)
                    .contentHash(contentHash)
                    .userId(userId)
                    .build();

            documentRepository.save(loaded);
            processedFiles.add(finalFilename);
        }

        String message = processedFiles.isEmpty()
                ? NO_NEW_DOCUMENTS_UPLOADED
                : DOCUMENTS_UPLOADED + String.join(", ", processedFiles);

        return RagResponse.createSuccessful(message);
    }

    private String getExtensionOrTxt(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx == -1 || idx == filename.length() - 1) {
            return TXT_EXTENSION;
        }
        return filename.substring(idx + 1);
    }

    private void acceptWithRetry(VectorStore vs, List<Document> part, int attempts, long sleepMs) {
        RuntimeException last = null;
        for (int i = 0; i < attempts; i++) {
            try {
                vs.accept(part);
                return;
            } catch (RuntimeException e) {
                last = e;
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw last;
    }
}
