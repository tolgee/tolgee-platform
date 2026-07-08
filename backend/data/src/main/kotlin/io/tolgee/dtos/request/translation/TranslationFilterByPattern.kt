package io.tolgee.dtos.request.translation

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException

data class TranslationFilterByPattern(
  val languageTag: String?,
  val pattern: String,
) {
  companion object {
    /**
     * Parses "languageTag,pattern" values. Splits on the first comma only — the pattern part
     * may contain commas. `*` as the language tag means "any language".
     */
    fun parseList(strings: List<String>): List<TranslationFilterByPattern> =
      strings.map {
        val separatorIndex = it.indexOf(',')
        if (separatorIndex < 1) throw BadRequestException(Message.FILTER_PATTERN_NOT_VALID)
        val tag = it.substring(0, separatorIndex)
        TranslationFilterByPattern(
          languageTag = tag.takeUnless { t -> t == "*" },
          pattern = it.substring(separatorIndex + 1),
        )
      }
  }
}
