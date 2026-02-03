package io.tolgee.formats.compose.`in`

class ComposeStringUnescaper(
  private val string: Sequence<Char>,
  private val escapeMark: Char = '\\',
  private val toUnescape: Map<Char, String> = toUnescapeDefault,
) {
  private val initialState
    get() = State.NORMAL

  companion object {
    val defaultFactory = { string: String, isFirst: Boolean, isLast: Boolean ->
      ComposeStringUnescaper(string.asSequence()).result
    }

    private val toUnescapeDefault =
      mapOf(
        'n' to "\n",
        't' to "\t",
        'u' to "\\u",
        '\\' to "\\",
      )
  }

  val result: String
    get() =
      buildString {
        var state = initialState
        for (char in string) {
          state =
            when (state) {
              State.NORMAL ->
                when (char) {
                  escapeMark -> State.ESCAPED
                  else -> {
                    append(char)
                    state
                  }
                }

              State.ESCAPED -> {
                char.unescape().forEach { append(it) }
                State.NORMAL
              }
            }
        }

        when (state) {
          State.NORMAL -> {}
          State.ESCAPED -> append(escapeMark)
        }
      }

  private fun Char.unescape(): String {
    return toUnescape.getOrDefault(this, escapeMark + this.toString())
  }

  enum class State {
    NORMAL,
    ESCAPED,
  }
}
