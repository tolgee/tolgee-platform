package io.tolgee.dtos

import com.fasterxml.jackson.annotation.JsonSetter
import io.tolgee.configuration.tolgee.machineTranslation.LLMProviderInterface
import io.tolgee.model.enums.LLMProviderPriority
import io.tolgee.model.enums.LLMProviderType

data class LLMProviderDto(
  var id: Long,
  override var name: String,
  override var type: LLMProviderType,
  override var priority: LLMProviderPriority?,
  override var apiKey: String?,
  override var apiUrl: String?,
  override var model: String?,
  override var deployment: String?,
  override var keepAlive: String?,
  override var format: String?,
  override var pricePerMillionInput: Double?,
  override var pricePerMillionOutput: Double?,
  override var attempts: List<Int>?,
) : LLMProviderInterface {
  @JsonSetter("type")
  fun setType(type: String) {
    this.type = LLMProviderType.valueOf(type.uppercase())
  }
}
