package io.tolgee.formats.flutter.`in`

import io.tolgee.exceptions.ImportCannotParseFileException

class FlutterArbFileParseException(
  filename: String,
  cause: Exception,
) : ImportCannotParseFileException(filename, "Cannot parse arb file", cause)
