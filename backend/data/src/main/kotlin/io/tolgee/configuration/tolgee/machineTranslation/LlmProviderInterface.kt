package io.tolgee.configuration.tolgee.machineTranslation

import io.tolgee.configuration.annotations.DocProperty
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.LlmProviderType

interface LlmProviderInterface {
  @DocProperty(description = "Provider name")
  var name: String

  @DocProperty(description = "Provider type: openai or ollama")
  var type: LlmProviderType

  @DocProperty(description = "Provider priority")
  var priority: LlmProviderPriority?

  // general
  var apiKey: String?
  var apiUrl: String
  var model: String?

  // openai only
  var deployment: String?

  // ollama only
  var keepAlive: String?
  var format: String?

  // pricing
  var tokenPriceInCreditsInput: Double?
  var tokenPriceInCreditsOutput: Double?

  @DocProperty(description = "List of attempts (values are timeouts in seconds)")
  var attempts: List<Int>?
}
