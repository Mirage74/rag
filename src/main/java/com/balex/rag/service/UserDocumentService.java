package com.balex.rag.service;

import com.balex.rag.model.response.RagResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserDocumentService {
    RagResponse<String> processUploadedFiles(List<MultipartFile> files, Long userId);
}
