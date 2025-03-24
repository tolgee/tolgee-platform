package io.tolgee.configuration.tolgee.machineTranslation

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.llm-providers.ollama")
open class OllamaProperties(
  var apiKey: String? = null,
  var apiUrl: String? = null,
  var model: String? = null,
  var keepAlive: String? = null,
  var format: String? = null,
)
