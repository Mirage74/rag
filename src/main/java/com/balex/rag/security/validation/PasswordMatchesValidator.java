package com.balex.rag.security.validation;

import com.balex.rag.model.request.user.RegistrationUserRequest;
import com.balex.rag.utils.PasswordMatches;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, RegistrationUserRequest> {

    @Override
    public boolean isValid(RegistrationUserRequest request, ConstraintValidatorContext constraintValidatorContext) {
        return request.getPassword().equals(request.getConfirmPassword());
    }

}

