package io.tolgee.exceptions;

import io.tolgee.constants.Message;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.util.List;

public abstract class ErrorException extends RuntimeException {
    private final List<Serializable> params;

    private final String code;

    public ErrorException(Message message, List<Serializable> params) {
        this.params = params;
        this.code = message.getCode();
    }

    public ErrorException(Message message) {
        this.code = message.getCode();
        params = null;
    }

    public ErrorResponseBody getErrorResponseBody() {
        return new ErrorResponseBody(this.code, this.params);
    }

    public abstract HttpStatus getHttpStatus();

    public List<Serializable> getParams() {
        return this.params;
    }

    public String getCode() {
        return this.code;
    }
}
