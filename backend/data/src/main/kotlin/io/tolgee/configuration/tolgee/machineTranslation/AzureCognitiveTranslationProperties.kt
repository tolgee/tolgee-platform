package io.tolgee.configuration.tolgee.machineTranslation

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.machine-translation.azurecognitive")
open class AzureCognitiveTranslationProperties(
  override var defaultEnabled: Boolean = true,
  override var defaultPrimary: Boolean = false,
  var authKey: String? = null,
  var region: String? = null
) : MachineTranslationServiceProperties
