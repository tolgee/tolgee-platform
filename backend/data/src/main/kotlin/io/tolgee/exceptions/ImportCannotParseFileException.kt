package io.tolgee.exceptions

open class ImportCannotParseFileException(
  filename: String,
  val causeMessage: String?,
  cause: Exception? = null,
) : BadRequestException(io.tolgee.constants.Message.CANNOT_PARSE_FILE, listOf(filename, causeMessage ?: ""), cause)
