package io.tolgee.formats.escaping

/**
 * Escapes a plural form, so it doesn't break the full ICU string
 */
class PluralFormIcuEscaper(
  private val input: String,
  private val escapeHash: Boolean = false,
) {
  companion object {
    private const val ESCAPE_CHAR = '\''
  }

  private enum class State {
    StateText,
    StateEscaped,
    StateEscapedMaybe,
    StateEscapeEndMaybe,
  }

  private val escapableChars by lazy {
    val base = setOf('{', '}', '\'')
    if (escapeHash) {
      base + setOf('#')
    } else {
      base
    }
  }

  val escaped: String
    get() {
      var state = State.StateText
      val result = StringBuilder()
      var lastNeedsEscapeIndex: Int? = null

      input.forEachIndexed { index, char ->
        when (state) {
          State.StateText -> {
            if (char == ESCAPE_CHAR) {
              state = State.StateEscapedMaybe
            } else if (escapableChars.contains(char)) {
              result.append(ESCAPE_CHAR)
              state = State.StateEscaped
              lastNeedsEscapeIndex = result.length
            }
            result.append(char)
          }

          State.StateEscapedMaybe -> {
            if (char == ESCAPE_CHAR) {
              state = State.StateText
            } else if (escapableChars.contains(char)) {
              state = State.StateEscaped
              lastNeedsEscapeIndex = result.length
            } else {
              state = State.StateText
            }
            result.append(char)
          }

          State.StateEscaped -> {
            if (char == ESCAPE_CHAR) {
              state = State.StateEscapeEndMaybe
            } else {
              if (escapableChars.contains(char)) {
                lastNeedsEscapeIndex = result.length
              }
            }
            result.append(char)
          }

          State.StateEscapeEndMaybe -> {
            if (char == ESCAPE_CHAR) {
              state = State.StateEscaped
              result.append(ESCAPE_CHAR)
            } else {
              result.append(char)
              state = State.StateText
            }
          }
        }
      }

      if (state == State.StateEscaped) {
        result.insert(lastNeedsEscapeIndex!! + 1, ESCAPE_CHAR)
      }
      return result.toString()
    }
}
