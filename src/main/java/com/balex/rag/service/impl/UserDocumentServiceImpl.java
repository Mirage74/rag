package com.balex.rag.service.impl;

import com.balex.rag.model.LoadedDocument;
import com.balex.rag.model.response.RagResponse;
import com.balex.rag.repo.DocumentRepository;
import com.balex.rag.service.UserDocumentService;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class UserDocumentServiceImpl implements UserDocumentService {

    private final DocumentRepository documentRepository;
    private final VectorStore vectorStore;

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
                filename = "unknown";
            }
            final String finalFilename = filename;

            byte[] content;
            try {
                content = file.getBytes();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read file: " + finalFilename, e);
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
                c.getMetadata().put("user_id", userId);
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
                ? "No new documents uploaded"
                : "Documents uploaded: " + String.join(", ", processedFiles);

        return RagResponse.createSuccessful(message);
    }

    private String getExtensionOrTxt(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx == -1 || idx == filename.length() - 1) {
            return "txt";
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
