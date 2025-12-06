package com.balex.rag.service;

import com.balex.rag.model.UploadProgress;
import com.balex.rag.model.response.RagResponse;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

public interface UserDocumentService {
    RagResponse<String> processUploadedFiles(List<MultipartFile> files, Long userId);

    Flux<UploadProgress> processUploadedFilesWithProgress(List<MultipartFile> files, Long userId);
}