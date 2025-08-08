package io.tolgee.dtos.request.llmProvider

import com.fasterxml.jackson.annotation.JsonSetter
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.LlmProviderType

data class LlmProviderRequest(
  var name: String,
  var type: LlmProviderType,
  var apiUrl: String,
  var apiKey: String? = null,
  var priority: LlmProviderPriority? = null,
  var model: String? = null,
  var deployment: String? = null,
  var keepAlive: String? = null,
  var format: String? = null,
  var reasoningEffort: String? = null,
) {
  @JsonSetter("type")
  fun setType(type: String) {
    this.type = LlmProviderType.valueOf(type.uppercase())
  }
}
