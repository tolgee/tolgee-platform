package io.tolgee.formats.unity

/**
 * Escapes/unescapes literal braces for Unity Smart Strings. Unity's SmartFormat fork uses backslash
 * as the escape character (Settings.StringFormatCompatibility = false), so a literal brace is `\{` /
 * `\}` and a literal backslash is `\\`. The two directions are exact inverses.
 */
object UnitySmartFormatEscaper {
  fun escape(input: String): String {
    val result = StringBuilder(input.length)
    for (char in input) {
      if (char == '\\' || char == '{' || char == '}') {
        result.append('\\')
      }
      result.append(char)
    }
    return result.toString()
  }

  fun unescape(input: String): String {
    val result = StringBuilder(input.length)
    var escaped = false
    for (char in input) {
      if (escaped) {
        result.append(char)
        escaped = false
        continue
      }
      if (char == '\\') {
        escaped = true
        continue
      }
      result.append(char)
    }
    if (escaped) {
      result.append('\\')
    }
    return result.toString()
  }
}
