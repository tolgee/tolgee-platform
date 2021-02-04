package io.tolgee.dtos.request.validators.exceptions;

import io.tolgee.constants.Message;
import io.tolgee.dtos.request.validators.ValidationError;
import io.tolgee.dtos.request.validators.ValidationErrorType;

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
