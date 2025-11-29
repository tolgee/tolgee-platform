package io.tolgee.ee.service.prompt

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.sentry.Sentry
import io.tolgee.dtos.PromptResult
import io.tolgee.exceptions.LlmProviderNotReturnedJsonException
import io.tolgee.util.updateStringsInJson

class PromptResultParser(
  private val promptResult: PromptResult,
) {
  fun parse(): ParsedResult {
    Sentry.configureScope { scope -> scope.setContexts("llmDiagnostics", promptResult.diagnosticInfo) }

    val json = extractJsonFromResponse(promptResult.response)
    val output = json?.get("output")?.asText() ?: throw LlmProviderNotReturnedJsonException()
    val contextDescription = json.get("contextDescription")?.asText()

    return ParsedResult(
      promptResult = promptResult,
      output = output,
      parsedJson = json,
      contextDescription = contextDescription,
    )
  }

  private fun extractJsonFromResponse(content: String): JsonNode? {
    // attempting different strategies to find a json in the response
    val attempts =
      listOf<(String) -> String>(
        { it },
        { getJsonLike(it) },
        { getJsonLike(it.substringAfter("```").substringBefore("```")) },
      )
    for (attempt in attempts) {
      val result = parseJsonSafely(attempt.invoke(content))
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
      val result = jacksonObjectMapper().readValue<JsonNode>(content)
      updateStringsInJson(result) {
        // gpt-4.1 sometimes includes NIL,
        // which is invalid utf-8 character breaking DB saving
        it.replace("\u0000", "")
      }
    } catch (e: JsonProcessingException) {
      throw LlmProviderNotReturnedJsonException(e)
    }
  }

  data class ParsedResult(
    val promptResult: PromptResult,
    val parsedJson: JsonNode,
    val output: String,
    val contextDescription: String?,
  )
}
