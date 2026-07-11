package io.tolgee.dtos.request.translation

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.enums.qa.QaCheckType

data class TranslationFilterByQaCheckType(
  val languageTag: String,
  val checkType: QaCheckType,
) {
  companion object {
    fun parseList(strings: List<String>): List<TranslationFilterByQaCheckType> {
      return BaseFilterByKeyValue
        .parseList(
          strings,
          { QaCheckType.valueOf(it) },
          { BadRequestException(Message.FILTER_BY_VALUE_QA_CHECK_TYPE_NOT_VALID) },
        ).map { TranslationFilterByQaCheckType(it.first, it.second) }
    }
  }
}
