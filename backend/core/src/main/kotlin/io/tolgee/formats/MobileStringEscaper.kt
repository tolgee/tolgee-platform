package io.tolgee.formats

import io.tolgee.formats.xmlResources.XmlResourcesParsingConstants

class MobileStringEscaper(
  private val string: String,
  private val escapeApos: Boolean,
  private val keepPercentSignEscaped: Boolean,
  /**
   * We only support this non XML strings
   */
  private val quoteMoreWhitespaces: Boolean,
  private val escapeNewLines: Boolean,
  private val escapeQuotes: Boolean,
  // Android need 'u' and iOS 'U'
  private val utfSymbolCharacter: Char,
) {
  private enum class State {
    DEFAULT,
    SPACES,
    ESCAPE,
    PERCENT,
    UTF_SYMBOL,
  }

  private val stringBuilder = StringBuilder()
  private var state = State.DEFAULT
  private val spaces = StringBuilder()
  private val percents = StringBuilder()
  private val utfSymbol = StringBuilder()

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
    if (escapeNewLines) XmlResourcesParsingConstants.spacesWithoutNewLines else XmlResourcesParsingConstants.spaces

  private fun handleChar(char: Char) {
    when (state) {
      State.DEFAULT -> {
        when (char) {
          '\\' -> state = State.ESCAPE
          '%' -> {
            state = State.PERCENT
            percents.append(char)
          }

          '"' -> if (escapeQuotes) stringBuilder.append("\\\"") else stringBuilder.append(char)

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
        when (char) {
          in arrayOf('U', 'u') -> {
            state = State.UTF_SYMBOL
            utfSymbol.append(char)
          }

          '\\' -> {
            stringBuilder.append("\\\\")
            state = State.DEFAULT
          }

          'n' -> {
            stringBuilder.append("\\n")
            state = State.DEFAULT
          }

          else -> {
            stringBuilder.append("\\\\")
            state = State.DEFAULT
            handleChar(char)
          }
        }
      }

      State.SPACES -> {
        if (char in XmlResourcesParsingConstants.spaces) {
          spaces.append(char)
        } else {
          handleSpacesEnd(char)
        }
      }

      State.UTF_SYMBOL -> {
        if (char in '0'..'9' || char in 'a'..'f' || char in 'A'..'F') {
          utfSymbol.append(char)
          if (utfSymbol.length == 5) {
            val hex = utfSymbol.drop(1)
            stringBuilder.append("\\$utfSymbolCharacter$hex")
            utfSymbol.clear()
            state = State.DEFAULT
          }
        } else {
          stringBuilder.append("\\$utfSymbol")
          utfSymbol.clear()
          state = State.DEFAULT
          handleChar(char)
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
      val unescaped = percents.toString().replace("%%", "\\%")
      stringBuilder.append(unescaped)
    } else {
      stringBuilder.append(percents)
    }
    state = State.DEFAULT
    percents.clear()
    if (char != null) {
      handleChar(char)
    }
  }

  private fun handleSpacesEnd(char: Char?) {
    if (quoteMoreWhitespaces && spaces.length > 1) {
      stringBuilder.append("\"$spaces\"")
    } else {
      stringBuilder.append(spaces)
    }
    spaces.clear()
    state = State.DEFAULT
    char?.let { handleChar(it) }
  }
}
