package io.tolgee.configuration.tolgee.machineTranslation

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.machine-translation.tolgee")
open class TolgeeMachineTranslationProperties(
  override var defaultEnabled: Boolean = true,
  override var defaultPrimary: Boolean = false,
  var url: String? = "https://app.tolgee.io",
) : MachineTranslationServiceProperties
