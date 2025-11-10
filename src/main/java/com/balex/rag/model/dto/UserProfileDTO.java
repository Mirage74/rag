package com.balex.rag.model.dto;

import com.balex.rag.model.enums.RegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class UserProfileDTO implements Serializable {
    private Integer id;
    private String username;
    private String email;

    private RegistrationStatus registrationStatus;
    private LocalDateTime lastLogin;

    private String token;
    private String refreshToken;

}

