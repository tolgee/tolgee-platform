package io.tolgee.dtos.request.translation

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.enums.TranslationState

data class TranslationFilterByState(
  val languageTag: String,
  val state: TranslationState,
) {
  companion object {
    /**
     * Parses query string param provided by spring.
     *
     * When single value in format "en,TRANSLATED" is provided,
     * Spring parses the value as listOf("en", "TRANSLATED").
     *
     * When multiople filter values are provided e.g. "en,TRANSLATED", "de,REVIEWED", it
     * parses the value as listOf("en,TRANSLATED", "e,REVIEWED").
     *
     * This function handles both cases.
     */
    fun parseList(strings: List<String>): List<TranslationFilterByState> {
      if (isMultipleFilters(strings)) {
        return strings.map { parseComaSeparated(it) }
      }

      return parseSingleFilter(strings)
    }

    private fun parseSingleFilter(strings: List<String>): List<TranslationFilterByState> {
      val chunked = getChunked(strings)
      return chunked.map {
        val (tag, stateString) = it
        try {
          val state = TranslationState.valueOf(stateString)
          TranslationFilterByState(tag, state)
        } catch (e: IllegalArgumentException) {
          throw BadRequestException(Message.FILTER_BY_VALUE_STATE_NOT_VALID)
        }
      }
    }

    private fun isMultipleFilters(strings: List<String>): Boolean {
      return strings.all { it.contains(",") }
    }

    private fun getChunked(strings: List<String>): List<List<String>> {
      if (strings.size % 2 != 0) {
        throw BadRequestException(Message.FILTER_BY_VALUE_STATE_NOT_VALID)
      }
      return strings.chunked(2)
    }

    private fun parseComaSeparated(string: String): TranslationFilterByState {
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
