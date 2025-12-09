package com.balex.rag.service.impl;

import com.balex.rag.mapper.UserMapper;
import com.balex.rag.model.LoadedDocument;
import com.balex.rag.model.constants.ApiErrorMessage;
import com.balex.rag.model.dto.UserDTO;
import com.balex.rag.model.dto.UserSearchDTO;
import com.balex.rag.model.entity.LoadedDocumentInfo;
import com.balex.rag.model.entity.User;
import com.balex.rag.model.entity.UserInfo;
import com.balex.rag.model.exception.InvalidTokenException;
import com.balex.rag.model.exception.NotFoundException;
import com.balex.rag.model.request.user.NewUserRequest;
import com.balex.rag.model.request.user.UpdateUserRequest;
import com.balex.rag.model.response.PaginationResponse;
import com.balex.rag.model.response.RagResponse;
import com.balex.rag.repo.DocumentRepository;
import com.balex.rag.repo.UserRepository;
import com.balex.rag.repo.VectorStoreRepository;
import com.balex.rag.security.JwtTokenProvider;
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
import java.util.List;

import static com.balex.rag.model.constants.ApiConstants.USER_ROLE;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AccessValidator accessValidator;
    private final JwtTokenProvider jwtTokenProvider;
    private final DocumentRepository documentRepository;
    private final VectorStoreRepository vectorStoreRepository;

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
                .orElseThrow(() -> new NotFoundException(ApiErrorMessage.EMAIL_NOT_FOUND.getMessage(email)));

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(USER_ROLE))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public RagResponse<UserInfo> getUserInfo(String token) {
        User user = getUserInfoFromToken(token);

        List<LoadedDocumentInfo> loadedFiles = documentRepository
                .findByUserId(user.getId())
                .stream()
                .map(doc -> new LoadedDocumentInfo(doc.getId(), doc.getFilename()))
                .toList();

        UserInfo userInfo = new UserInfo(user.getId(),
                user.getUsername(),
                user.getEmail(),
                loadedFiles);

        return RagResponse.createSuccessful(userInfo);
    }

    @Override
    @Transactional
    public RagResponse<Integer> deleteUserDocuments(String token) {
        getUserInfoFromToken(token);
        User user = getUserInfoFromToken(token);

        List<LoadedDocument> documents = documentRepository.findByUserId(user.getId());

        if (documents.isEmpty()) {
            return RagResponse.createSuccessful(0);
        }

        // Удаляем чанки по user_id
        vectorStoreRepository.deleteByUserId(user.getId().longValue());

        // Удаляем записи из loaded_document
        documentRepository.deleteAll(documents);

        return RagResponse.createSuccessful(documents.size());
    }

    private User getUserInfoFromToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Token is empty or null");
        }

        String cleanToken = token.startsWith("Bearer ")
                ? token.substring(7)
                : token;

        String username = jwtTokenProvider.getUsername(cleanToken);

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidTokenException("User not found: " + username));
    }
}

