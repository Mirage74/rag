package com.balex.rag.model.dto;

import com.balex.rag.model.enums.RegistrationStatus;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserSearchDTO implements Serializable {

    private Integer id;
    private String username;
    private String email;
    private LocalDateTime created;
    private Boolean isDeleted;

    private RegistrationStatus registrationStatus;

}
