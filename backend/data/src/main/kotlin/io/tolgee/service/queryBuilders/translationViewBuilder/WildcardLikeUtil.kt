package io.tolgee.service.queryBuilders.translationViewBuilder

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException

object WildcardLikeUtil {
  const val ESCAPE_CHAR = '\\'

  // mirrored in webapp buildSearchRequestParams.ts — a change here needs a frontend update
  const val MAX_PATTERN_LENGTH = 500
  const val MAX_WILDCARDS = 5
  const val MAX_PATTERNS_PER_PARAM = 20

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
    if (patterns.size > MAX_PATTERNS_PER_PARAM) {
      throw BadRequestException(Message.FILTER_PATTERN_NOT_VALID, listOf(MAX_PATTERNS_PER_PARAM))
    }
    patterns.forEach {
      if (it.isEmpty()) {
        throw BadRequestException(Message.FILTER_PATTERN_NOT_VALID)
      }
      if (it.length > MAX_PATTERN_LENGTH) {
        throw BadRequestException(Message.FILTER_PATTERN_NOT_VALID, listOf(MAX_PATTERN_LENGTH))
      }
      if (it.count { char -> char == '*' } > MAX_WILDCARDS) {
        throw BadRequestException(Message.FILTER_PATTERN_NOT_VALID, listOf(MAX_WILDCARDS))
      }
    }
  }
}
