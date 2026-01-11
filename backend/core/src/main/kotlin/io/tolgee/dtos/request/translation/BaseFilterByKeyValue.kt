package io.tolgee.dtos.request.translation

import io.tolgee.exceptions.BadRequestException

object BaseFilterByKeyValue {
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
  fun <T> parseList(
    strings: List<String>,
    valueConverter: (String) -> T,
    exceptionSupplier: () -> BadRequestException,
  ): List<Pair<String, T>> {
    if (strings.all { it.contains(",") }) {
      return strings.map { parseCommaSeparated(it, valueConverter, exceptionSupplier) }
    }
    return parseSingleFilter(strings, valueConverter, exceptionSupplier)
  }

  private fun <T> parseSingleFilter(
    strings: List<String>,
    valueConverter: (String) -> T,
    exceptionSupplier: () -> BadRequestException,
  ): List<Pair<String, T>> {
    if (strings.size % 2 != 0) throw exceptionSupplier()
    return strings.chunked(2).map {
      if (it.size != 2) throw exceptionSupplier()
      it[0] to
        try {
          valueConverter(it[1])
        } catch (e: Exception) {
          throw exceptionSupplier()
        }
    }
  }

  private fun <T> parseCommaSeparated(
    string: String,
    valueConverter: (String) -> T,
    exceptionSupplier: () -> BadRequestException,
  ): Pair<String, T> {
    val parts = string.split(",")
    if (parts.size != 2) throw exceptionSupplier()
    return parts[0] to
      try {
        valueConverter(parts[1])
      } catch (e: Exception) {
        throw exceptionSupplier()
      }
  }
}
