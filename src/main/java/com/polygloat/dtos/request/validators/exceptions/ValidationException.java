package com.polygloat.dtos.request.validators.exceptions;

import com.polygloat.constants.Message;
import com.polygloat.dtos.request.validators.ValidationError;
import com.polygloat.dtos.request.validators.ValidationErrorType;
import lombok.Getter;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class ValidationException extends RuntimeException {
    @Getter
    private Set<ValidationError> validationErrors = new LinkedHashSet<>();

    public ValidationException(Message message, String... parameters) {
        this.validationErrors.add(new ValidationError(ValidationErrorType.CUSTOM_VALIDATION, message, parameters));
    }

    public ValidationException(ValidationErrorType type, String... parameters) {
        this.validationErrors.add(new ValidationError(type, Message.VALIDATION_ERROR, parameters));
    }

    public ValidationException(Collection<ValidationError> validationErrors) {
        this.validationErrors.addAll(validationErrors);
    }
}
