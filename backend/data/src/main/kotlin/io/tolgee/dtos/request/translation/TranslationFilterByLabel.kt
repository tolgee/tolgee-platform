package io.tolgee.dtos.request.translation

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException

data class TranslationFilterByLabel(
  val languageTag: String,
  val labelId: Long,
) {
  companion object {
    fun parseList(strings: List<String>): List<TranslationFilterByLabel> {
      return BaseFilterByKeyValue
        .parseList(
          strings,
          { it.toLong() },
          { BadRequestException(Message.FILTER_BY_VALUE_LABEL_NOT_VALID) },
        ).map { TranslationFilterByLabel(it.first, it.second) }
    }
  }
}
