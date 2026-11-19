package io.tolgee.configuration.tolgee.machineTranslation

import io.tolgee.configuration.annotations.DocProperty
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.LlmProviderType

interface LlmProviderInterface {
  @DocProperty(description = "Provider name")
  var name: String

  @DocProperty(description = "Provider type")
  var type: LlmProviderType

  @DocProperty(description = "Provider priority")
  var priority: LlmProviderPriority?

  // general
  @DocProperty(description = "Provider API key")
  var apiKey: String?

  @DocProperty(description = "Provider API url")
  var apiUrl: String?

  @DocProperty(description = "Provider model name")
  var model: String?

  @DocProperty(description = "Provider format")
  var format: String?

  // azure openai only
  @DocProperty(description = "Provider deployment (azure only)")
  var deployment: String?

  // openai, anthropic
  @DocProperty(
    description = """Reasoning effort. `reasoning_effort` option for OpenAI API.
    `output_config.effort` for Anthropic API.""",
  )
  var reasoningEffort: String?

  // anthropic
  @DocProperty(
    description = """Thinking type for Anthropic API (`thinking.type`),
    e.g. `disabled`, `between_tools` or `adaptive`. Which types a model accepts differs per model.
    When empty, no `thinking` is sent and the model uses its default.""",
  )
  var thinking: String?

  var maxTokens: Long

  // pricing
  @DocProperty(hidden = true)
  var tokenPriceInCreditsInput: Double?

  @DocProperty(hidden = true)
  var tokenPriceInCreditsOutput: Double?

  @DocProperty(description = "List of attempts (values are timeouts in seconds)")
  var attempts: List<Int>?
}
