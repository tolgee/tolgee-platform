package io.tolgee.formats

/**
 * It escapes controlling characters in ICU message, so it's not interpreted when in comes from other formats
 */
class IcuMessageEscaper(
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
              result.append(ESCAPE_CHAR)
              result.append(ESCAPE_CHAR)
            } else if (escapableChars.contains(char)) {
              state = State.StateEscaped
              result.append(ESCAPE_CHAR)
              result.append(ESCAPE_CHAR)
              lastNeedsEscapeIndex = result.length
            } else {
              state = State.StateText
            }
            result.append(char)
          }

          State.StateEscaped -> {
            if (char == ESCAPE_CHAR) {
              state = State.StateText
              result.append(ESCAPE_CHAR)
              result.append(ESCAPE_CHAR)
            } else {
              if (escapableChars.contains(char)) {
                lastNeedsEscapeIndex = result.length
              }
            }
            result.append(char)
          }
        }
      }

      if (state == State.StateEscaped) {
        result.insert(lastNeedsEscapeIndex!! + 1, ESCAPE_CHAR)
      }
      return result.toString()
    }
}
