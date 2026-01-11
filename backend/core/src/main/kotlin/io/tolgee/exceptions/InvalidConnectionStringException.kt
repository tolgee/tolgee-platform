package io.tolgee.exceptions

import io.tolgee.constants.Message

class InvalidConnectionStringException : BadRequestException(Message.INVALID_CONNECTION_STRING)
