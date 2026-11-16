package io.tolgee.ee.service.aiReview.scoring

import io.tolgee.ee.service.prompt.LlmJsonExtractor
import io.tolgee.model.aiReview.AiReviewIssue
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.math.RoundingMode

object ReviewResultParser {
  private val MAX_POINTS = BigDecimal(Int.MAX_VALUE)

  fun parse(
    response: String,
    objectMapper: ObjectMapper,
  ): List<AiReviewIssue> {
    val json = LlmJsonExtractor.extract(response, objectMapper) ?: throw ReviewParseException()
    val issues = json.get("issues")
    if (issues == null || !issues.isArray) throw ReviewParseException()
    return (0 until issues.size()).map { parseIssue(issues.get(it)) }
  }

  private fun parseIssue(node: JsonNode): AiReviewIssue {
    if (!node.isObject) throw ReviewParseException()
    val severity = node.requiredText("severity")
    return AiReviewIssue(
      severity = severity,
      category = node.optionalText("category"),
      message = node.requiredText("message"),
      points = parsePoints(node.get("points")) ?: DefaultSeverityRanges.fallbackPoints(severity),
    )
  }

  private fun parsePoints(node: JsonNode?): Int? {
    val number = numberOf(node) ?: return null
    if (number.signum() < 0) return null
    return number.setScale(0, RoundingMode.HALF_UP).min(MAX_POINTS).toInt()
  }

  private fun numberOf(node: JsonNode?): BigDecimal? {
    if (node == null || node.isNull) return null
    if (node.isNumber) return node.decimalValue()
    if (node.isString) return node.asString().trim().toBigDecimalOrNull()
    return null
  }

  private fun JsonNode.requiredText(field: String): String {
    val value = optionalText(field)
    if (value.isNullOrBlank()) throw ReviewParseException()
    return value
  }

  private fun JsonNode.optionalText(field: String): String? {
    val value = get(field) ?: return null
    if (value.isNull) return null
    if (!value.isValueNode) throw ReviewParseException()
    return value.asString()
  }
}
