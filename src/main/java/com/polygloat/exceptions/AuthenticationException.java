package com.polygloat.exceptions;

import com.polygloat.constants.Message;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.util.List;

public class AuthenticationException extends ErrorException {
    public AuthenticationException(Message message, List<Serializable> params) {
        super(message, params);
    }

    public AuthenticationException(Message message) {
        super(message);
    }

    public HttpStatus getHttpStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
