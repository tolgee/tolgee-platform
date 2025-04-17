package io.tolgee.configuration.tolgee.machineTranslation

import io.tolgee.configuration.annotations.DocProperty
import io.tolgee.model.enums.LLMProviderPriority
import io.tolgee.model.enums.LLMProviderType

interface LLMProviderInterface {
  @DocProperty(description = "Provider name")
  var name: String

  @DocProperty(description = "Provider type: openai or ollama")
  var type: LLMProviderType

  @DocProperty(description = "Provider priority")
  var priority: LLMProviderPriority?

  // general
  var apiKey: String?
  var apiUrl: String?
  var model: String?

  // openai only
  var deployment: String?

  // ollama only
  var keepAlive: String?
  var format: String?

  // pricing
  var pricePerMillionInput: Double?
  var pricePerMillionOutput: Double?

  @DocProperty(description = "List of attempts (values are timeouts in seconds)")
  var attempts: List<Int>?
}
