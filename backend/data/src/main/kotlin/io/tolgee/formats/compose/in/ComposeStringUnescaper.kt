package io.tolgee.formats.compose.`in`

class ComposeStringUnescaper(
  private val string: Sequence<Char>,
  private val escapeMark: Char = '\\',
  private val toUnescape: Map<Char, String> = toUnescapeDefault
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
    get() = resultSeq.fold(StringBuilder()) { acc, c ->
      acc.append(c)
    }.toString()

  val resultSeq: Sequence<Char>
    get() = sequence {
      var state = initialState
      for (char in string) {
        state = when (state) {
          State.NORMAL -> when (char) {
            escapeMark -> State.ESCAPED
            else -> {
              yield(char)
              state
            }
          }
          State.ESCAPED -> {
            char.unescape().forEach { yield(it) }
            State.NORMAL
          }
        }
      }

      when (state) {
        State.NORMAL -> {}
        State.ESCAPED -> yield(escapeMark)
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
