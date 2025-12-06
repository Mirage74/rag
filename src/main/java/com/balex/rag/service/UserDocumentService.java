package com.balex.rag.service;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface UserDocumentService {
    SseEmitter processUploadedFilesWithSse(List<MultipartFile> files, Long userId);
}