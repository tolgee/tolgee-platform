package io.tolgee.formats.escaping

/**
 * It escapes controlling characters in ICU message, so it's not interpreted when in comes from other formats
 */
class IcuMessageEscapeRemover(
  private val input: String,
  private val isPlural: Boolean = false,
) {
  companion object {
    private const val ESCAPE_CHAR = '\''
  }

  enum class State {
    StateText,
    StateEscapedMaybe,
    StateEscaped,
  }

  private val escapableChars by lazy {
    val base = setOf('{', '}', '\'')
    if (isPlural) {
      base + setOf('#')
    } else {
      base
    }
  }

  val escapeRemoved: String
    get() {
      val result = StringBuilder()
      var state = State.StateText

      for (ch in input) {
        when (state) {
          State.StateText -> {
            if (ch == ESCAPE_CHAR) {
              state = State.StateEscapedMaybe
            } else {
              result.append(ch)
            }
          }

          State.StateEscapedMaybe -> {
            if (ch == ESCAPE_CHAR) {
              state = State.StateText
            } else if (escapableChars.contains(ch)) {
              state = State.StateEscaped
            } else {
              state = State.StateText
              result.append(ESCAPE_CHAR)
            }
            result.append(ch)
          }

          State.StateEscaped -> {
            if (ch == ESCAPE_CHAR) {
              state = State.StateText
            } else {
              result.append(ch)
            }
          }
        }
      }
      if (state == State.StateEscapedMaybe) {
        result.append(ESCAPE_CHAR)
      }
      return result.toString()
    }
}
