package com.polygloat.exceptions;

import com.polygloat.constants.Message;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.util.List;

public abstract class ErrorException extends RuntimeException {
    @Getter
    private final List<Serializable> params;

    @Getter
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

}
