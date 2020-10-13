package com.polygloat.exceptions;

import com.polygloat.constants.Message;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.util.List;

public class InternalException extends ErrorException {
    public InternalException(Message message, List<Serializable> params) {
        super(message, params);
    }

    public InternalException(Message message) {
        super(message);
    }

    public HttpStatus getHttpStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
