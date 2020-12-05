package io.polygloat.dtos.request.validators.exceptions;

import io.polygloat.constants.Message;
import io.polygloat.dtos.request.validators.ValidationError;
import io.polygloat.dtos.request.validators.ValidationErrorType;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class ValidationException extends RuntimeException {
    private Set<ValidationError> validationErrors = new LinkedHashSet<>();

    public ValidationException(Message message, String... parameters) {
        this.validationErrors.add(new ValidationError(ValidationErrorType.CUSTOM_VALIDATION, message, parameters));
    }

    public ValidationException(Collection<ValidationError> validationErrors) {
        this.validationErrors.addAll(validationErrors);
    }

    public Set<ValidationError> getValidationErrors() {
        return this.validationErrors;
    }
}
