package io.tolgee.model.batch

/**
 * Represents a single result from the OpenAI Batch API response.
 * Stored as part of the JSONB `results` column on [OpenAiBatchJobTracker].
 */
data class OpenAiBatchResult(
  val customId: String,
  val keyId: Long,
  val languageId: Long,
  val translatedText: String?,
  val contextDescription: String?,
  val promptTokens: Long = 0,
  val completionTokens: Long = 0,
  val error: String? = null,
)
