package io.tolgee.formats.android.out

import io.tolgee.formats.android.AndroidParsingConstants

class AndroidStringsEscaper(
  private val string: String,
  private val escapeApos: Boolean,
  private val keepPercentSignEscaped: Boolean,
  /**
   * We only support this non XML strings
   */
  private val quoteMoreWhitespaces: Boolean,
  private val escapeNewLines: Boolean,
) {
  private enum class State {
    DEFAULT,
    SPACES,
    ESCAPE,
    PERCENT,
  }

  private val stringBuilder = StringBuilder()
  private var state = State.DEFAULT
  private val spaces = StringBuilder()
  private val percents = StringBuilder()

  fun escape(): String {
    string.forEach { char ->
      handleChar(char)
    }

    if (state == State.SPACES) {
      handleSpacesEnd(null)
    }

    if (state == State.PERCENT) {
      handlePercentEnd(null)
    }

    return stringBuilder.toString()
  }

  private val relevantSpaces =
    if (escapeNewLines) AndroidParsingConstants.spacesWithoutNewLines else AndroidParsingConstants.spaces

  private fun handleChar(char: Char) {
    when (state) {
      State.DEFAULT -> {
        when (char) {
          '\\' -> state = State.ESCAPE
          '%' -> {
            state = State.PERCENT
            percents.append(char)
          }

          '"' -> stringBuilder.append("\\\"")

          '\'' -> if (escapeApos) stringBuilder.append("\\'") else stringBuilder.append(char)
          in relevantSpaces -> {
            state = State.SPACES
            spaces.append(char)
          }

          // if escapeNewLines is false, this part is unreachable, singe '\n' is in relevantSpaces
          '\n' -> stringBuilder.append("\\n")

          else -> stringBuilder.append(char)
        }
      }

      State.ESCAPE -> {
        stringBuilder.append("\\\\")
        if (char != '\\') stringBuilder.append(char)
        state = State.DEFAULT
      }

      State.SPACES -> {
        if (char in AndroidParsingConstants.spaces) {
          spaces.append(char)
        } else {
          handleSpacesEnd(char)
        }
      }

      State.PERCENT -> {
        if (char == '%') {
          percents.append(char)
        } else {
          handlePercentEnd(char)
        }
      }
    }
  }

  private fun handlePercentEnd(char: Char?) {
    if (!keepPercentSignEscaped) {
      val unescaped = percents.toString().replace("%%", "%")
      stringBuilder.append(unescaped)
    } else {
      stringBuilder.append(percents)
    }
    state = State.DEFAULT
    if (char != null) {
      handleChar(char)
    }
  }

  private fun handleSpacesEnd(char: Char?) {
    if (quoteMoreWhitespaces && spaces.length > 1) {
      stringBuilder.append("\"$spaces\"")
    } else {
      stringBuilder.append(spaces.first())
    }
    spaces.clear()
    state = State.DEFAULT
    char?.let { handleChar(it) }
  }
}
