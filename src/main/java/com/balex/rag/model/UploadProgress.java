package com.balex.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadProgress {
    private int percent;
    private int processedFiles;
    private int totalFiles;
    private String currentFile;
    private String status; // "processing", "completed", "error"
}