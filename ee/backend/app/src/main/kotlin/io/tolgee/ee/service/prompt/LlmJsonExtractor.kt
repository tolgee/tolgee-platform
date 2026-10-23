package io.tolgee.ee.service.prompt

import io.tolgee.util.updateStringsInJson
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

object LlmJsonExtractor {
  fun extract(
    content: String,
    objectMapper: ObjectMapper,
  ): JsonNode? {
    // attempting different strategies to find a json in the response
    val attemptFns =
      listOf<(String) -> String>(
        { it },
        { getJsonLike(it) },
        { getJsonLike(it.substringAfter("```").substringBefore("```")) },
      )
    for (attemptFn in attemptFns) {
      val result = parseJsonSafely(attemptFn(content), objectMapper)
      if (result != null) {
        return result
      }
    }
    return null
  }

  private fun getJsonLike(content: String): String {
    return "{${content.substringAfter("{").substringBeforeLast("}")}}"
  }

  private fun parseJsonSafely(
    content: String,
    objectMapper: ObjectMapper,
  ): JsonNode? {
    return try {
      val result = objectMapper.readValue<JsonNode>(content)
      updateStringsInJson(result) {
        // gpt-4.1 sometimes includes NIL,
        // which is invalid utf-8 character breaking DB saving
        it.replace("\u0000", "")
      }
    } catch (_: JacksonException) {
      null
    }
  }
}
