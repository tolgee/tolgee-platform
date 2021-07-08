package io.tolgee.exceptions

import io.tolgee.constants.Message
import org.springframework.http.HttpStatus

class PermissionException : ErrorException(Message.OPERATION_NOT_PERMITTED) {
    override val httpStatus: HttpStatus
        get() = HttpStatus.FORBIDDEN
}
