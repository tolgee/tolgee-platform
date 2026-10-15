package io.tolgee.dtos

enum class LlmResponseSchema(
  val schemaName: String,
  val schema: Map<String, Any>,
) {
  TRANSLATION(
    schemaName = "simple_response",
    schema =
      mapOf(
        "type" to "object",
        "properties" to
          mapOf(
            "output" to mapOf("type" to "string"),
            "contextDescription" to mapOf("type" to "string"),
          ),
        "required" to listOf("output", "contextDescription"),
        "additionalProperties" to false,
      ),
  ),
  AI_REVIEW(
    schemaName = "ai_review_response",
    schema =
      mapOf(
        "type" to "object",
        "properties" to
          mapOf(
            "issues" to
              mapOf(
                "type" to "array",
                "items" to
                  mapOf(
                    "type" to "object",
                    "properties" to
                      mapOf(
                        "severity" to mapOf("type" to "string", "enum" to listOf("critical", "major", "minor")),
                        "category" to mapOf("type" to "string"),
                        "message" to mapOf("type" to "string"),
                        "suggestion" to mapOf("type" to listOf("string", "null")),
                        "points" to mapOf("type" to "integer"),
                      ),
                    "required" to listOf("severity", "category", "message", "suggestion", "points"),
                    "additionalProperties" to false,
                  ),
              ),
          ),
        "required" to listOf("issues"),
        "additionalProperties" to false,
      ),
  ),
}
