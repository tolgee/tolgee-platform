package io.polygloat.exceptions;

import io.polygloat.constants.Message;
import org.springframework.http.HttpStatus;

public class PermissionException extends ErrorException {
    public PermissionException() {
        super(Message.OPERATION_NOT_PERMITTED);
    }

    public HttpStatus getHttpStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
