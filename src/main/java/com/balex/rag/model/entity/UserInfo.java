package com.balex.rag.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo implements Serializable {

    Integer id;

    String username;

    String email;

    List<LoadedDocumentInfo> loadedFiles;

}
