package io.tolgee.service.queryBuilders.translationViewBuilder

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException

/**
 * Translates user-facing wildcard patterns (`*` = any sequence) into SQL LIKE patterns.
 */
object WildcardLikeUtil {
  const val ESCAPE_CHAR = '\\'

  const val MAX_PATTERN_LENGTH = 500
  const val MAX_WILDCARDS = 5
  const val MAX_PATTERNS_PER_PARAM = 20

  /**
   * `%`, `_` and the escape char in the input are matched literally; without any `*` the
   * pattern gets contains semantics (consistent with the `search` param), with `*` it is
   * anchored (`cart*` = starts with).
   */
  fun toLikePattern(input: String): String {
    val escaped =
      input
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
    if (!escaped.contains('*')) return "%$escaped%"
    return escaped.replace('*', '%')
  }

  fun validatePatterns(patterns: List<String>?) {
    if (patterns == null) return
    if (patterns.size > MAX_PATTERNS_PER_PARAM) throw BadRequestException(Message.FILTER_PATTERN_NOT_VALID)
    patterns.forEach {
      if (it.length > MAX_PATTERN_LENGTH) throw BadRequestException(Message.FILTER_PATTERN_NOT_VALID)
      if (it.count { char -> char == '*' } > MAX_WILDCARDS) {
        throw BadRequestException(Message.FILTER_PATTERN_NOT_VALID)
      }
    }
  }
}
