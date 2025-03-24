package io.tolgee.configuration.tolgee.machineTranslation

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.llm")
class LLMProperties {
  var providers: MutableList<LLMProvider> = mutableListOf()

  class LLMProvider {
    @DocProperty(description = "Provider name")
    var name: String? = null
    @DocProperty(description = "Provider type: openai or ollama")
    var type: String? = null

    // general
    var apiKey: String? = null
    var apiUrl: String? = null
    var endpoint: String? = null

    // ollama only
    var model: String? = null
    var keepAlive: String? = null
    var format: String? = null
  }
}
