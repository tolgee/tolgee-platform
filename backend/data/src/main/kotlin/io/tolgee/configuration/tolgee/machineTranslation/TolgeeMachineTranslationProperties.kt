package io.tolgee.configuration.tolgee.machineTranslation

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.llm-providers.openai")
open class TolgeeMachineTranslationProperties(
  var apiKey: String? = null,
  var apiUrl: String? = null,
)
