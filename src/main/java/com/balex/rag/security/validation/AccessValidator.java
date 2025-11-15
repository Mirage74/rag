package com.balex.rag.security.validation;

import com.balex.rag.model.constants.ApiErrorMessage;
import com.balex.rag.model.entity.User;
import com.balex.rag.model.exception.InvalidDataException;
import com.balex.rag.model.exception.InvalidPasswordException;
import com.balex.rag.model.exception.NotFoundException;
import com.balex.rag.repo.UserRepository;
import com.balex.rag.service.model.exception.DataExistException;
import com.balex.rag.utils.ApiUtils;
import com.balex.rag.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.nio.file.AccessDeniedException;

@Component
@RequiredArgsConstructor
public class AccessValidator {
    private final UserRepository userRepository;
    private final ApiUtils apiUtils;

    public void validateNewUser(String username, String email, String password, String confirmPassword) {
        userRepository.findByUsername(username).ifPresent(existingUser -> {
            throw new DataExistException(ApiErrorMessage.USERNAME_ALREADY_EXISTS.getMessage(username));
        });

        userRepository.findByEmail(email).ifPresent(existingUser -> {
            throw new DataExistException(ApiErrorMessage.EMAIL_ALREADY_EXISTS.getMessage(email));
        });

        if (!password.equals(confirmPassword)) {
            throw new InvalidDataException(ApiErrorMessage.MISMATCH_PASSWORDS.getMessage());
        }

        if (PasswordUtils.isNotValidPassword(password)) {
            throw new InvalidPasswordException(ApiErrorMessage.INVALID_PASSWORD.getMessage());
        }
    }

    @SneakyThrows
    public void validateOwnerAccess(Integer ownerId) {
        Integer currentUserId = apiUtils.getUserIdFromAuthentication();

        if (!currentUserId.equals(ownerId)) {
            throw new AccessDeniedException(ApiErrorMessage.HAVE_NO_ACCESS.getMessage());
        }
    }

}

