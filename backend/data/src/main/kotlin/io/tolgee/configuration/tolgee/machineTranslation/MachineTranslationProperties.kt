package io.tolgee.configuration.tolgee.machineTranslation

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.machine-translation")
open class MachineTranslationProperties(
  var google: GoogleMachineTranslationProperties = GoogleMachineTranslationProperties(),
  var aws: AwsMachineTranslationProperties = AwsMachineTranslationProperties(),
  var freeCreditsAmount: Long = -1,
)
