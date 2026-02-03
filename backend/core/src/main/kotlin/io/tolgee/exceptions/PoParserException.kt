package io.tolgee.exceptions

class PoParserException(
  message: String = "Error",
  line: Int,
  position: Int,
) : Throwable() {
  override val message = "$message on line $line, position: $position"
}
