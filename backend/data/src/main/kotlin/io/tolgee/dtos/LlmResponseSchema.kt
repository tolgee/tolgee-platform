package io.tolgee.dtos

data class LlmResponseSchema(
  val name: String,
  val schema: Map<String, Any>,
)
