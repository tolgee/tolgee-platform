package io.tolgee.dtos.request

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.enums.TranslationState

data class FilterByState(
  val languageTag: String,
  val state: TranslationState,
) {
  companion object {
    fun valueOf(string: String): FilterByState {
      val values = string.split(",")
      if (values.size != 2) {
        throw BadRequestException(Message.FILTER_BY_VALUE_STATE_NOT_VALID)
      }
      val tag = values[0]
      try {
        val state = TranslationState.valueOf(values[1])
        return FilterByState(tag, state)
      } catch (e: IllegalArgumentException) {
        throw BadRequestException(Message.FILTER_BY_VALUE_STATE_NOT_VALID)
      }
    }
  }
}
