package com.balex.rag.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoadedDocumentInfo implements Serializable {
    Long id;
    String fileName;
}
