package io.tolgee.formats.escaping

import io.tolgee.formats.escaping.ForceIcuEscaper.State.*

/**
 * This class forcefully escapes syntax of ICU messages.
 * It's ported from Stapan Granat's JS code
 */
class ForceIcuEscaper(private val input: String, private val escapeHash: Boolean = false) {
  enum class State {
    StateText,
    StateEscapedMaybe,
  }

  private val escapable by lazy {
    val base = setOf('{', '}')
    if (escapeHash) {
      base + setOf('#')
    } else {
      base
    }
  }

  private val escapeChar = '\''

  val escaped by lazy {
    var state: State = StateText
    val result = StringBuilder()

    for (char in input) {
      when (state) {
        StateText ->
          if (char == escapeChar) {
            result.append(char)
            state = StateEscapedMaybe
          } else if (escapable.contains(char)) {
            result.append(escapeChar)
            result.append(char)
            result.append(escapeChar)
          } else {
            result.append(char)
          }

        StateEscapedMaybe -> {
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
          }
          state = StateText
        }
      }
    }

    if (state == StateEscapedMaybe) {
      result.append(escapeChar)
    }

    result.toString()
  }
}
