package io.tolgee.dtos.request.llmProvider

import com.fasterxml.jackson.annotation.JsonSetter
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.LlmProviderType
import jakarta.validation.constraints.Size

data class LlmProviderRequest(
  @field:Size(max = 255)
  var name: String,
  var type: LlmProviderType,
  @field:Size(max = 2047)
  var apiUrl: String,
  @field:Size(max = 511)
  var apiKey: String? = null,
  var priority: LlmProviderPriority? = null,
  @field:Size(max = 255)
  var model: String? = null,
  @field:Size(max = 255)
  var deployment: String? = null,
  @field:Size(max = 255)
  var keepAlive: String? = null,
  @field:Size(max = 255)
  var format: String? = null,
  @field:Size(max = 255)
  var reasoningEffort: String? = null,
) {
  @JsonSetter("type")
  fun setType(type: String) {
    this.type = LlmProviderType.valueOf(type.uppercase())
  }
}
