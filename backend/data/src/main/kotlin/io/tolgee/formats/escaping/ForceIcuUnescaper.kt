package io.tolgee.formats.escaping

/**
 * This class forcefully de-escapes syntax of ICU messages.
 * It's ported from Stapan Granat's JS code
 */
class ForceIcuUnescaper(private val input: String) {
  private val stateText = 0
  private val stateEscapedMaybe = 1
  private val stateEscaped = 2

  private val escapable = setOf('{', '}', '#')
  private val escapeChar = '\''

  val unescaped by lazy {
    var state = stateText
    val result = StringBuilder()

    for (char in input) {
      when (state) {
        stateText ->
          if (char == escapeChar) {
            state = stateEscapedMaybe
          } else {
            result.append(char)
          }

        stateEscapedMaybe -> {
          if (escapable.contains(char)) {
            state = stateEscaped
          } else if (char == escapeChar) {
            state = stateText
          } else {
            result.append(escapeChar)
            state = stateText
          }
          result.append(char)
        }

        stateEscaped ->
          if (char == escapeChar) {
            state = stateText
          } else {
            result.append(char)
          }
      }
    }

    result.toString()
  }
}
