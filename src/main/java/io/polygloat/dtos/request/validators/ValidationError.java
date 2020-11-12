package io.polygloat.dtos.request.validators;

import io.polygloat.constants.Message;
import lombok.Getter;

public class ValidationError {
    @Getter
    private String[] parameters;

    @Getter
    private ValidationErrorType type;

    @Getter
    private Message message;

    public ValidationError(ValidationErrorType type, Message message, String... parameters) {
        this.parameters = parameters;
        this.message = message;
        this.type = type;
    }
}
