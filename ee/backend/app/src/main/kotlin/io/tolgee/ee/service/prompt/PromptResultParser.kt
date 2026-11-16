package io.tolgee.ee.service.prompt

import io.tolgee.dtos.PromptResult
import io.tolgee.exceptions.LlmProviderNotReturnedJsonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

class PromptResultParser(
  private val promptResult: PromptResult,
  private val objectMapper: ObjectMapper,
) {
  fun parse(): ParsedResult {
    val json = LlmJsonExtractor.extract(promptResult.response, objectMapper)
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

  data class ParsedResult(
    val promptResult: PromptResult,
    val parsedJson: JsonNode,
    val output: String,
    val contextDescription: String?,
  )
}
