package io.tolgee.dtos.request.translation

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.enums.TranslationState

data class TranslationFilterByState(
  val languageTag: String,
  val state: TranslationState,
) {
  companion object {
    fun valueOf(string: String): TranslationFilterByState {
      val (tag, stateString) = parseStateString(string)
      try {
        val state = TranslationState.valueOf(stateString)
        return TranslationFilterByState(tag, state)
      } catch (e: IllegalArgumentException) {
        throw BadRequestException(Message.FILTER_BY_VALUE_STATE_NOT_VALID)
      }
    }

    private fun parseStateString(string: String): Pair<String, String> {
      val values = string.split(",")
      validateValues(values)
      return Pair(values[0], values[1])
    }

    private fun validateValues(values: List<String>) {
      if (values.size != 2) {
        throw BadRequestException(Message.FILTER_BY_VALUE_STATE_NOT_VALID)
      }
    }
  }
}
