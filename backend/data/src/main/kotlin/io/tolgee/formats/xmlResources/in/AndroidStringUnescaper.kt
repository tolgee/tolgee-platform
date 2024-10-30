package io.tolgee.formats.xmlResources.`in`

import io.tolgee.formats.xmlResources.XmlResourcesParsingConstants

class AndroidStringUnescaper(private val string: String, private val isFirst: Boolean, private val isLast: Boolean) {
  companion object {
    private val toUnescape =
      mapOf(
        'n' to "\n",
        '\'' to "\'",
        '"' to "\"",
        't' to "\t",
        'u' to "\\u",
        '\\' to "\\",
      )
    private val spacesToTrim = XmlResourcesParsingConstants.spaces
  }

  val unescaped: String by lazy {
    string.forEach { char ->
      when (char) {
        '\\' ->
          when (state) {
            State.NORMAL -> state = State.ESCAPED
            State.ESCAPED -> {
              result.append('\\')
              resetIgnoreSpace()
              state = State.NORMAL
            }
          }

        '"' ->
          when (state) {
            State.ESCAPED -> {
              result.append('"')
              resetIgnoreSpace()
              state = State.NORMAL
            }

            State.NORMAL ->
              quotingState =
                when (quotingState) {
                  QuotingState.USING_SPACE, QuotingState.IGNORING_SPACE -> QuotingState.QUOTED
                  QuotingState.QUOTED -> QuotingState.USING_SPACE
                }
          }

        in spacesToTrim ->
          when (quotingState) {
            QuotingState.USING_SPACE -> {
              result.append(char)
              quotingState = QuotingState.IGNORING_SPACE
            }

            QuotingState.QUOTED -> {
              result.append(char)
            }

            QuotingState.IGNORING_SPACE -> {}
          }

        else -> {
          if (state == State.ESCAPED) {
            if (char in toUnescape.keys) {
              result.append(toUnescape[char])
            } else {
              result.append(char)
            }
            state = State.NORMAL
          } else {
            result.append(char)
          }
          resetIgnoreSpace()
        }
      }
    }

    if (quotingState == QuotingState.IGNORING_SPACE && result.lastOrNull() in spacesToTrim && isLast) {
      result.deleteCharAt(result.length - 1)
    }

    result.toString()
  }

  private fun resetIgnoreSpace() {
    if (quotingState == QuotingState.IGNORING_SPACE) {
      quotingState = QuotingState.USING_SPACE
    }
  }

  enum class State {
    NORMAL,
    ESCAPED,
  }

  enum class QuotingState {
    USING_SPACE,
    QUOTED,
    IGNORING_SPACE,
  }

  private var state = State.NORMAL
  private var quotingState = if (isFirst) QuotingState.IGNORING_SPACE else QuotingState.USING_SPACE

  private val result = StringBuilder()
}
