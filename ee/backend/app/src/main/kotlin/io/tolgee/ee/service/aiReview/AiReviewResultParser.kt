package io.tolgee.ee.service.aiReview

import io.tolgee.ee.service.prompt.PromptResultParser
import io.tolgee.model.aiReview.AiReviewIssue
import io.tolgee.model.enums.aiReview.AiReviewIssueSeverity
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import kotlin.math.roundToInt

class AiReviewResultParser(
  private val objectMapper: ObjectMapper,
) {
  data class Result(
    val score: Int,
    val issues: List<AiReviewIssue>,
  )

  class InvalidOutput : RuntimeException()

  fun parse(response: String): Result {
    val json = unwrapOutput(PromptResultParser.extractJson(response, objectMapper) ?: throw InvalidOutput())
    val issuesNode = json.get("issues")
    if (issuesNode == null || !issuesNode.isArray) throw InvalidOutput()
    val issues = (0 until issuesNode.size()).map { parseIssue(issuesNode.get(it)) }
    val score = (MAX_SCORE - issues.sumOf { it.points }).coerceIn(0, MAX_SCORE)
    return Result(score, issues)
  }

  private fun unwrapOutput(json: JsonNode): JsonNode {
    if (json.has("issues")) return json
    val output = json.get("output")?.takeIf { it.isString }?.asString() ?: return json
    return PromptResultParser.extractJson(output, objectMapper) ?: json
  }

  private fun parseIssue(node: JsonNode): AiReviewIssue {
    if (!node.isObject) throw InvalidOutput()
    val severity = parseSeverity(node.get("severity"))
    val message = node.get("message")?.scalarText()?.takeIf { it.isNotBlank() } ?: throw InvalidOutput()
    val category = node.get("category")?.scalarText()?.takeIf { it.isNotBlank() } ?: DEFAULT_CATEGORY
    val suggestion = node.get("suggestion")?.scalarText()?.takeIf { it.isNotBlank() }
    return AiReviewIssue(
      severity = severity,
      category = category.take(MAX_CATEGORY_LENGTH),
      message = message.take(MAX_TEXT_LENGTH),
      suggestion = suggestion?.take(MAX_TEXT_LENGTH),
      points = parsePoints(node.get("points"), severity),
    )
  }

  private fun parseSeverity(node: JsonNode?): AiReviewIssueSeverity {
    val name = node?.scalarText()?.trim()?.uppercase() ?: throw InvalidOutput()
    return AiReviewIssueSeverity.entries.find { it.name == name } ?: throw InvalidOutput()
  }

  private fun parsePoints(
    node: JsonNode?,
    severity: AiReviewIssueSeverity,
  ): Int {
    val number = node?.toPointsNumber()?.takeIf { it.isFinite() } ?: return severity.minPoints
    return severity.clampPoints(number.roundToInt())
  }

  private fun JsonNode.toPointsNumber(): Double? {
    if (isNumber) return asDouble()
    if (isString) return asString().trim().toDoubleOrNull()
    return null
  }

  private fun JsonNode.scalarText(): String? {
    if (isNull || !isValueNode) return null
    return asString()
  }

  companion object {
    private const val MAX_SCORE = 100
    private const val MAX_TEXT_LENGTH = 2000
    private const val MAX_CATEGORY_LENGTH = 100
    private const val DEFAULT_CATEGORY = "other"
  }
}
