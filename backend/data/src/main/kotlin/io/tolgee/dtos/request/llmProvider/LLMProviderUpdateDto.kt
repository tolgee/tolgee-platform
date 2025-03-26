package io.tolgee.dtos.request.llmProvider

import io.tolgee.configuration.tolgee.machineTranslation.LLMProviderInterface
import io.tolgee.model.enums.LLMProviderType

data class LLMProviderUpdateDto(
  override var name: String,
  override var type: LLMProviderType,
  override var priority: String?,
  override var apiKey: String?,
  override var apiUrl: String?,
  override var model: String?,
  override var deployment: String?,
  override var keepAlive: String?,
  override var format: String?,
) : LLMProviderInterface
