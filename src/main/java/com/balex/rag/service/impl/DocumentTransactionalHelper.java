package com.balex.rag.service.impl;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Helper component to handle transactional file processing.
 *
 * This separate bean is necessary because Spring's @Transactional relies on proxies,
 * and self-invocation within the same class bypasses the proxy, causing transactions
 * to not be applied.
 */
@Component
public class DocumentTransactionalHelper {

    /**
     * Processes a single file within a transaction boundary.
     *
     * @param file the file to process
     * @param userId the user ID
     * @param filename the filename
     * @param processor the processing function to execute
     * @return true if file was processed, false if skipped
     */
    @Transactional
    public boolean processFileInTransaction(MultipartFile file,
                                            Long userId,
                                            String filename,
                                            FileProcessor processor) {
        return processor.process(file, userId, filename);
    }

    @FunctionalInterface
    public interface FileProcessor {
        boolean process(MultipartFile file, Long userId, String filename);
    }
}