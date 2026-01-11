package io.tolgee.dtos.request.translation

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.enums.TranslationState

data class TranslationFilterByState(
  val languageTag: String,
  val state: TranslationState,
) {
  companion object {
    fun parseList(strings: List<String>): List<TranslationFilterByState> {
      return BaseFilterByKeyValue
        .parseList(
          strings,
          { TranslationState.valueOf(it) },
          { BadRequestException(Message.FILTER_BY_VALUE_STATE_NOT_VALID) },
        ).map { TranslationFilterByState(it.first, it.second) }
    }
  }
}
