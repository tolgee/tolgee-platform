package io.tolgee.helpers

object IcuParamsReplacer {
  fun doNothing(string: String): ReplaceIcuResult {
    return ReplaceIcuResult(string, false, emptyMap())
  }

  /**
   * Replaces ICU params with strings which are not handled by translators
   */
  fun extract(string: String): ReplaceIcuResult {
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
    val params: Map<String, String>
  ) {
    fun addParams(string: String): String {
      var replaced = string
      params.forEach { (placeholder, text) ->
        replaced = replaced.replace(placeholder, text)
      }
      return replaced
    }
  }
}
