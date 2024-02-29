package io.tolgee.formats.escaping

/**
 * This class forcefully escapes syntax of ICU messages.
 * It's ported from Stapan Granat's JS code
 */
class ForceIcuEscaper(private val input: String) {
  private val stateText = 0
  private val stateEscapedMaybe = 1

  private val escapable = setOf('{', '}', '#')
  private val escapeChar = '\''

  val escaped by lazy {
    var state = stateText
    val result = StringBuilder()

    for (char in input) {
      when (state) {
        stateText ->
          if (char == escapeChar) {
            result.append(char)
            state = stateEscapedMaybe
          } else if (escapable.contains(char)) {
            result.append(escapeChar)
            result.append(char)
            result.append(escapeChar)
          } else {
            result.append(char)
          }

        stateEscapedMaybe ->
          if (escapable.contains(char)) {
            // escape the EscapeChar
            result.append(escapeChar)
            // append() another layer of escape on top
            result.append(escapeChar)
            result.append(char)
            result.append(escapeChar)
          } else if (char == escapeChar) {
            // two escape chars - escape both
            result.append(escapeChar)
            result.append(char)
            result.append(escapeChar)
          } else {
            result.append(char)
          }.also {
            state = stateText
          }
      }
    }

    if (state == stateEscapedMaybe) {
      result.append(escapeChar)
    }

    result.toString()
  }
}
