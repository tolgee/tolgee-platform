package com.polygloat.exceptions;

import com.polygloat.constants.Message;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.util.List;

public class BadRequestException extends ErrorException {
    public BadRequestException(Message message, List<Serializable> params) {
        super(message, params);
    }

    public BadRequestException(Message message) {
        super(message);
    }

    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
