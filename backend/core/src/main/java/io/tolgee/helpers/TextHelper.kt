package io.tolgee.helpers

import java.util.regex.MatchResult
import java.util.regex.Pattern

object TextHelper {
  @JvmStatic
  fun splitOnNonEscapedDelimiter(
    string: String,
    delimiter: Char?,
  ): List<String> {
    if (delimiter == null) {
      return listOf(string)
    }

    val result = ArrayList<String>()
    var actual = StringBuilder()
    for (i in string.indices) {
      val character = string[i]
      if (character == delimiter && !isCharEscaped(i, string)) {
        result.add(removeEscapes(actual.toString()))
        actual = StringBuilder()
        continue
      }
      actual.append(string[i])
    }
    result.add(removeEscapes(actual.toString()))
    return result
  }

  private fun isCharEscaped(
    position: Int,
    fullString: String,
  ): Boolean {
    if (position == 0) {
      return false
    }
    var pos = position
    var escapeCharsCount = 0
    while (pos > -1 && fullString[pos - 1] == '\\') {
      escapeCharsCount++
      pos--
    }
    return escapeCharsCount % 2 == 1
  }

  private fun removeEscapes(text: String): String {
    return Pattern.compile("\\\\?\\\\?").matcher(text).replaceAll { match: MatchResult ->
      if (match.group() == "\\\\") {
        // this seems strange. We need to escape it once more for the replace logic
        return@replaceAll "\\\\"
      }
      ""
    }
  }

  fun replaceIcuParams(string: String): ReplaceIcuResult {
    val result = StringBuilder()
    val buffer = StringBuilder()
    val params = mutableMapOf<String, String>()
    var isEscaped = false
    var openCount = 0
    var wasComplex = false

    for (char in string) {
      if (isEscaped) {
        isEscaped = false
        if (arrayOf('{', '}', '\'').contains(char)) {
          buffer.append(char)
          continue
        }

        // if character is not escapable, then we want to add the ' char as well
        // since it can be used as apostrophe
        // e.g. That's a nice tnetennba!
        buffer.append("'$char")
        continue
      }

      if (char == '\'') {
        isEscaped = true
        continue
      }

      if (char == '{') {
        if (openCount < 1) {
          result.append(buffer)
          buffer.clear()
        }
        buffer.append(char)
        openCount++
        continue
      }

      if (openCount > 0 && char == '}') {
        if (openCount > 1) {
          wasComplex = true
        }
        openCount--
        buffer.append(char)

        if (openCount < 1) {
          val paramPlaceholder = "{xx${params.size}xx}"
          params[paramPlaceholder] = buffer.toString()
          buffer.clear()
          result.append(paramPlaceholder)
        }
        continue
      }

      buffer.append(char)
    }
    result.append(buffer)
    return ReplaceIcuResult(result.toString(), wasComplex, params)
  }

  data class ReplaceIcuResult(
    val text: String,
    val isComplex: Boolean,
    val params: Map<String, String>,
  )
}
