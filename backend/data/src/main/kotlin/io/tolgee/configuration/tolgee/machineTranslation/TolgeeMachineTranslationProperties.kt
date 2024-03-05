package io.tolgee.configuration.tolgee.machineTranslation

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.machine-translation.tolgee")
open class TolgeeMachineTranslationProperties(
  override var defaultEnabled: Boolean = true,
  override var defaultPrimary: Boolean = true,
  @DocProperty(hidden = true)
  var url: String? = "https://app.tolgee.io",
) : MachineTranslationServiceProperties
