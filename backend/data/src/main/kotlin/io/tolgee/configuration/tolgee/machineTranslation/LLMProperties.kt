package io.tolgee.configuration.tolgee.machineTranslation

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.llm")
class LLMProperties {
  var providers: MutableList<LLMProvider> = mutableListOf()

  class LLMProvider {
    @DocProperty(description = "Provider name")
    var name: String = "default"

    @DocProperty(description = "Provider type: openai or ollama")
    var type: String? = null

    @DocProperty(description = "Provider priority")
    var priority: String? = null

    // general
    var apiKey: String? = null
    var apiUrl: String? = null
    var model: String? = null

    // openai only
    var deployment: String? = null

    // ollama only
    var keepAlive: String? = null
    var format: String? = null
  }
}
