package com.balex.rag.service.impl;

import com.balex.rag.mapper.UserMapper;
import com.balex.rag.model.constants.ApiErrorMessage;
import com.balex.rag.model.dto.UserDTO;
import com.balex.rag.model.dto.UserSearchDTO;
import com.balex.rag.model.entity.User;
import com.balex.rag.model.exception.NotFoundException;
import com.balex.rag.model.request.user.NewUserRequest;
import com.balex.rag.model.request.user.UpdateUserRequest;
import com.balex.rag.model.response.PaginationResponse;
import com.balex.rag.model.response.RagResponse;
import com.balex.rag.repo.UserRepository;
import com.balex.rag.security.validation.AccessValidator;
import com.balex.rag.service.UserService;
import com.balex.rag.service.model.exception.DataExistException;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;

import static com.balex.rag.model.constants.ApiConstants.USER_ROLE;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AccessValidator accessValidator;

    @Override
    @Transactional(readOnly = true)
    public RagResponse<UserDTO> getById(@NotNull Integer userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new NotFoundException(ApiErrorMessage.USER_NOT_FOUND_BY_ID.getMessage(userId)));

        return RagResponse.createSuccessful(userMapper.toDto(user));
    }

    @Override
    @Transactional
    public RagResponse<UserDTO> createUser(@NotNull NewUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DataExistException(ApiErrorMessage.EMAIL_ALREADY_EXISTS.getMessage(request.getEmail()));
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DataExistException(ApiErrorMessage.USERNAME_ALREADY_EXISTS.getMessage(request.getUsername()));
        }

        User user = userMapper.createUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User savedUser = userRepository.save(user);

        return RagResponse.createSuccessful(userMapper.toDto(savedUser));
    }

    @Override
    @Transactional
    public RagResponse<UserDTO> updateUser(@NotNull Integer userId, UpdateUserRequest request) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new NotFoundException(ApiErrorMessage.USER_NOT_FOUND_BY_ID.getMessage(userId)));

        if (!user.getUsername().equals(request.getUsername()) && userRepository.existsByUsername(request.getUsername())) {
            throw new DataExistException(ApiErrorMessage.USERNAME_ALREADY_EXISTS.getMessage(request.getUsername()));
        }

        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new DataExistException(ApiErrorMessage.EMAIL_ALREADY_EXISTS.getMessage(request.getEmail()));
        }

        userMapper.updateUser(user, request);
        user = userRepository.save(user);

        return RagResponse.createSuccessful(userMapper.toDto(user));
    }

    @Override
    @Transactional
    public void softDeleteUser(Integer userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new NotFoundException(ApiErrorMessage.USER_NOT_FOUND_BY_ID.getMessage(userId)));

        accessValidator.validateOwnerAccess(userId);

        user.setDeleted(true);
        userRepository.save(user);

    }

    @Override
    @Transactional(readOnly = true)
    public RagResponse<PaginationResponse<UserSearchDTO>> findAllUsers(Pageable pageable) {
        Page<UserSearchDTO> users = userRepository.findAll(pageable)
                .map(userMapper::toUserSearchDto);

        PaginationResponse<UserSearchDTO> paginationResponse = new PaginationResponse<>(
                users.getContent(),
                new PaginationResponse.Pagination(
                        users.getTotalElements(),
                        pageable.getPageSize(),
                        users.getNumber() + 1,
                        users.getTotalPages()
                )
        );

        return RagResponse.createSuccessful(paginationResponse);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return getUserDetails(email, userRepository);
    }

    static UserDetails getUserDetails(String email, UserRepository userRepository) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ApiErrorMessage.EMAIL_NOT_FOUND.getMessage()));

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(USER_ROLE))
        );
    }

}

