package com.balex.rag.service;

import com.balex.rag.model.dto.UserDTO;
import com.balex.rag.model.dto.UserSearchDTO;
import com.balex.rag.model.entity.UserInfo;
import com.balex.rag.model.request.user.NewUserRequest;
import com.balex.rag.model.request.user.UpdateUserRequest;
import com.balex.rag.model.response.PaginationResponse;
import com.balex.rag.model.response.RagResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {

    RagResponse<UserDTO> getById(@NotNull Integer userId);

    RagResponse<UserDTO> createUser(@NotNull NewUserRequest request);

    RagResponse<UserDTO> updateUser(@NotNull Integer postId, @NotNull UpdateUserRequest request);

    void softDeleteUser(Integer userId);

    RagResponse<PaginationResponse<UserSearchDTO>> findAllUsers(Pageable pageable);

    RagResponse<UserInfo> getUserInfo(String token);

}



