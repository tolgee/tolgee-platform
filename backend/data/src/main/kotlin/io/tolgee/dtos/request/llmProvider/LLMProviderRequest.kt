package io.tolgee.dtos.request.llmProvider

import com.fasterxml.jackson.annotation.JsonSetter
import io.tolgee.model.enums.LLMProviderPriority
import io.tolgee.model.enums.LLMProviderType

data class LLMProviderRequest(
  var name: String,
  var type: LLMProviderType,
  var apiUrl: String,
  var priority: LLMProviderPriority? = null,
  var apiKey: String? = null,
  var model: String? = null,
  var deployment: String? = null,
  var keepAlive: String? = null,
  var format: String? = null,
) {
  @JsonSetter("type")
  fun setType(type: String) {
    this.type = LLMProviderType.valueOf(type.uppercase())
  }
}
