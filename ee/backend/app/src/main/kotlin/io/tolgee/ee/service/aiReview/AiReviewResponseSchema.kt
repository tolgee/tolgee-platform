package io.tolgee.ee.service.aiReview

import io.tolgee.dtos.LlmResponseSchema

object AiReviewResponseSchema {
  private val ISSUE =
    mapOf(
      "type" to "object",
      "properties" to
        mapOf(
          "severity" to mapOf("type" to "string"),
          "category" to mapOf("anyOf" to listOf(mapOf("type" to "string"), mapOf("type" to "null"))),
          "message" to mapOf("type" to "string"),
          "points" to mapOf("type" to "number"),
        ),
      "required" to listOf("severity", "category", "message", "points"),
      "additionalProperties" to false,
    )

  val SCHEMA =
    LlmResponseSchema(
      name = "review_response",
      schema =
        mapOf(
          "type" to "object",
          "properties" to mapOf("issues" to mapOf("type" to "array", "items" to ISSUE)),
          "required" to listOf("issues"),
          "additionalProperties" to false,
        ),
    )
}
