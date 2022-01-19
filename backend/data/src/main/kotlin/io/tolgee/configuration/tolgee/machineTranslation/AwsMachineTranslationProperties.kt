package io.tolgee.configuration.tolgee.machineTranslation

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.machine-translation.aws")
open class AwsMachineTranslationProperties(
  override var defaultEnabled: Boolean = true,
  override var defaultPrimary: Boolean = false,
  var accessKey: String? = null,
  var secretKey: String? = null,
  var region: String? = "eu-central-1"
) : MachineTranslationServiceProperties
