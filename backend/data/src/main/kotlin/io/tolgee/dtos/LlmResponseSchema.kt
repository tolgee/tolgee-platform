package io.tolgee.dtos

data class LlmResponseSchema(
  val name: String,
  val schema: Map<String, Any>,
) {
  companion object {
    val TRANSLATION =
      LlmResponseSchema(
        name = "simple_response",
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
      )
  }
}
