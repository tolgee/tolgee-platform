package io.tolgee.ee.service.prompt

import io.tolgee.dtos.PromptResult
import io.tolgee.exceptions.LlmProviderNotReturnedJsonException
import io.tolgee.util.updateStringsInJson
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

class PromptResultParser(
  private val promptResult: PromptResult,
  private val objectMapper: ObjectMapper,
) {
  fun parse(): ParsedResult {
    val json = extractJsonFromResponse(promptResult.response)
    val output = json?.get("output")?.asScalarString() ?: throw LlmProviderNotReturnedJsonException()
    val contextDescription = json.get("contextDescription")?.asScalarString()

    return ParsedResult(
      promptResult = promptResult,
      output = output,
      parsedJson = json,
      contextDescription = contextDescription,
    )
  }

  // asString() throws on objects and arrays, so a wrong-shaped field would escape as JsonNodeException
  private fun JsonNode.asScalarString(): String {
    if (!isValueNode) throw LlmProviderNotReturnedJsonException()
    return asString()
  }

  private fun extractJsonFromResponse(content: String): JsonNode? {
    // attempting different strategies to find a json in the response
    val attemptFns =
      listOf<(String) -> String>(
        { it },
        { getJsonLike(it) },
        { getJsonLike(it.substringAfter("```").substringBefore("```")) },
      )
    for (attemptFn in attemptFns) {
      val result = parseJsonSafely(attemptFn(content))
      if (result != null) {
        return result
      }
    }
    return null
  }

  private fun getJsonLike(content: String): String {
    return "{${content.substringAfter("{").substringBeforeLast("}")}}"
  }

  private fun parseJsonSafely(content: String): JsonNode? {
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

  data class ParsedResult(
    val promptResult: PromptResult,
    val parsedJson: JsonNode,
    val output: String,
    val contextDescription: String?,
  )
}
