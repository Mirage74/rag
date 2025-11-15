package com.balex.rag.service.impl;

import com.balex.rag.mapper.UserMapper;
import com.balex.rag.model.constants.ApiErrorMessage;
import com.balex.rag.model.dto.UserProfileDTO;
import com.balex.rag.model.entity.RefreshToken;
import com.balex.rag.model.entity.User;
import com.balex.rag.model.exception.InvalidDataException;
import com.balex.rag.model.request.user.LoginRequest;
import com.balex.rag.model.request.user.RegistrationUserRequest;
import com.balex.rag.model.response.RagResponse;
import com.balex.rag.repo.UserRepository;
import com.balex.rag.security.JwtTokenProvider;
import com.balex.rag.security.validation.AccessValidator;
import com.balex.rag.service.AuthService;
import com.balex.rag.service.RefreshTokenService;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final AccessValidator accessValidator;


    @Override
    public RagResponse<UserProfileDTO> login(@NotNull LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new InvalidDataException(ApiErrorMessage.INVALID_USER_OR_PASSWORD.getMessage());
        }

        User user = userRepository.findUserByEmailAndDeletedFalse(request.getEmail())
                .orElseThrow(() -> new InvalidDataException(ApiErrorMessage.INVALID_USER_OR_PASSWORD.getMessage()));

        RefreshToken refreshToken = refreshTokenService.generateOrUpdateRefreshToken(user);
        String token = jwtTokenProvider.generateToken(user);
        UserProfileDTO userProfileDTO = userMapper.toUserProfileDto(user, token, refreshToken.getToken());
        userProfileDTO.setToken(token);

        return RagResponse.createSuccessfulWithNewToken(userProfileDTO);
    }

    @Override
    public RagResponse<UserProfileDTO> refreshAccessToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenService.validateAndRefreshToken(refreshTokenValue);
        User user = refreshToken.getUser();

        String accessToken = jwtTokenProvider.generateToken(user);

        return RagResponse.createSuccessfulWithNewToken(
                userMapper.toUserProfileDto(user, accessToken, refreshToken.getToken())
        );
    }

    @Override
    public RagResponse<UserProfileDTO> registerUser(@NotNull RegistrationUserRequest request) {

        accessValidator.validateNewUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getConfirmPassword()
        );

        User newUser = userMapper.fromDto(request);
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(newUser);

        RefreshToken refreshToken  = refreshTokenService.generateOrUpdateRefreshToken(newUser);
        String token = jwtTokenProvider.generateToken(newUser);
        UserProfileDTO userProfileDTO = userMapper.toUserProfileDto(newUser, token, refreshToken.getToken());
        userProfileDTO.setToken(token);

        return RagResponse.createSuccessfulWithNewToken(userProfileDTO);
    }

}

