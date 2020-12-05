package io.polygloat.dtos.request.validators;

import io.polygloat.constants.Message;

public class ValidationError {
    private String[] parameters;

    private ValidationErrorType type;

    private Message message;

    public ValidationError(ValidationErrorType type, Message message, String... parameters) {
        this.parameters = parameters;
        this.message = message;
        this.type = type;
    }

    public String[] getParameters() {
        return this.parameters;
    }

    public ValidationErrorType getType() {
        return this.type;
    }

    public Message getMessage() {
        return this.message;
    }
}
