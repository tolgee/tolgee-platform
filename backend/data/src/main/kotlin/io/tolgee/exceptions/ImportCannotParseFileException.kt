package io.tolgee.exceptions

class ImportCannotParseFileException(filename: String, causeMessage: String?) :
  BadRequestException(io.tolgee.constants.Message.CANNOT_PARSE_FILE, listOf(filename, causeMessage ?: ""))
