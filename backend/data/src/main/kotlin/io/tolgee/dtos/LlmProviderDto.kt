package io.tolgee.dtos

import com.fasterxml.jackson.annotation.JsonSetter
import io.tolgee.configuration.tolgee.machineTranslation.LlmProviderInterface
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.LlmProviderType

data class LlmProviderDto(
  var id: Long,
  override var name: String,
  override var type: LlmProviderType,
  override var priority: LlmProviderPriority?,
  override var apiKey: String?,
  private var rawApiUrl: String?,
  override var model: String?,
  override var deployment: String?,
  override var format: String?,
  override var reasoningEffort: String?,
  override var tokenPriceInCreditsInput: Double?,
  override var tokenPriceInCreditsOutput: Double?,
  override var attempts: List<Int>?,
  override var maxTokens: Long,
) : LlmProviderInterface {
  @JsonSetter("type")
  fun setType(type: String) {
    this.type = LlmProviderType.valueOf(type.uppercase())
  }

  override var apiUrl: String?
    get() = rawApiUrl?.removeSuffix("/")
    set(value) {
      rawApiUrl = value
    }
}
