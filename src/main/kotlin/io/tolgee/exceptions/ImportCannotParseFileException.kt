package io.tolgee.exceptions

import io.tolgee.constants.Message

class ImportCannotParseFileException(filename: String?, causeMessage: String?) :
        BadRequestException(Message.CANNOT_PARSE_FILE, listOf(filename, causeMessage)) {
}
