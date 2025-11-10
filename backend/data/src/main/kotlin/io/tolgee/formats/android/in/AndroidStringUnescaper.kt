package io.tolgee.formats.android.`in`

import io.tolgee.formats.xmlResources.XmlResourcesParsingConstants

class AndroidStringUnescaper(
  private val string: Sequence<Char>,
  private val isFirst: Boolean,
  private val isLast: Boolean,
  private val quotationMark: Char = '"',
  private val escapeMark: Char = '\\',
  private val spacesToTrim: Set<Char> = XmlResourcesParsingConstants.spaces,
  private val toUnescape: Map<Char, String> = toUnescapeDefault,
) {
  private val initialState
    get() = if (isFirst) State.AFTER_SPACE else State.NORMAL

  companion object {
    val defaultFactory = { string: String, isFirst: Boolean, isLast: Boolean ->
      AndroidStringUnescaper(string.asSequence(), isFirst, isLast).result
    }

    private val toUnescapeDefault =
      mapOf(
        'n' to "\n",
        '\'' to "\'",
        '"' to "\"",
        't' to "\t",
        'u' to "\\u",
        '\\' to "\\",
      )
  }

  var state = initialState

  var space: Char? = null

  val result: String by lazy {
    buildString {
      for (char in string) {
        state = handleCharacter(state, char)
      }

      when (state) {
        State.NORMAL -> {}
        State.AFTER_SPACE -> {
          val lastSpace = space
          if (lastSpace != null && !isLast) append(lastSpace)
        }

        State.ESCAPED -> {
          // Android deletes the last backslash if it is the last character
        }

        State.QUOTED -> {
          // Quoted text was not closed
        }

        State.QUOTED_ESCAPED -> {}
      }
    }
  }

  private fun StringBuilder.handleCharacter(
    state: State,
    char: Char,
  ): State {
    return when (state) {
      State.NORMAL ->
        when (char) {
          escapeMark -> State.ESCAPED
          quotationMark -> State.QUOTED
          in spacesToTrim -> {
            space = char
            State.AFTER_SPACE
          }

          else -> {
            append(char)
            State.NORMAL
          }
        }

      State.AFTER_SPACE -> {
        when (char) {
          in spacesToTrim -> State.AFTER_SPACE
          else -> {
            val lastSpace = space
            if (lastSpace != null) {
              append(lastSpace)
              space = null
            }
            handleCharacter(State.NORMAL, char)
          }
        }
      }

      State.ESCAPED -> {
        append(char.unescape())
        when (char) {
          in spacesToTrim -> State.AFTER_SPACE
          else -> State.NORMAL
        }
      }

      State.QUOTED ->
        when (char) {
          escapeMark -> State.QUOTED_ESCAPED
          quotationMark -> State.NORMAL
          else -> {
            append(char)
            State.QUOTED
          }
        }

      State.QUOTED_ESCAPED -> {
        append(char.unescape())
        State.QUOTED
      }
    }
  }

  private fun Char.unescape(): String {
    // Android always deletes the backslash even if the escaped character is not valid
    return toUnescape.getOrDefault(this, this.toString())
  }

  enum class State {
    NORMAL,
    AFTER_SPACE,
    ESCAPED,
    QUOTED,
    QUOTED_ESCAPED,
  }
}
