package io.tolgee.dtos.request.llmProvider

import com.fasterxml.jackson.annotation.JsonSetter
import io.tolgee.configuration.tolgee.machineTranslation.LLMProviderInterface
import io.tolgee.model.enums.LLMProviderType

data class LLMProviderCreateDto(
  override var name: String,
  override var type: LLMProviderType,
  override var priority: String?,
  override var apiKey: String?,
  override var apiUrl: String?,
  override var model: String?,
  override var deployment: String?,
  override var keepAlive: String?,
  override var format: String?,
) : LLMProviderInterface {
  @JsonSetter("type")
  fun setType(type: String) {
    this.type = LLMProviderType.valueOf(type.uppercase())
  }
}
