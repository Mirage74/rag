package com.balex.rag.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

import static com.balex.rag.model.constants.ApiConstants.MAX_FILES_ALLOWED_FOR_LOAD;

@Data
@NoArgsConstructor
public class UserInfo implements Serializable {

    Integer id;

    String username;

    String email;

    List<LoadedDocumentInfo> loadedFiles;

    Integer maxLoadedFiles = MAX_FILES_ALLOWED_FOR_LOAD;

    public UserInfo(Integer id, String username, String email, List<LoadedDocumentInfo> loadedFiles) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.loadedFiles = loadedFiles;
    }

}
