package com.balex.rag.service;

import com.balex.rag.model.dto.UserProfileDTO;
import com.balex.rag.model.request.user.LoginRequest;
import com.balex.rag.model.request.user.RegistrationUserRequest;
import com.balex.rag.model.response.RagResponse;

public interface AuthService {

    RagResponse<UserProfileDTO> login(LoginRequest request);

    RagResponse<UserProfileDTO> refreshAccessToken(String refreshToken);

    RagResponse<UserProfileDTO> registerUser(RegistrationUserRequest request);

}
