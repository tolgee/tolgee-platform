package io.tolgee.configuration.tolgee.machineTranslation

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.NestedConfigurationProperty

@DocProperty(
  prefix = "tolgee.machine-translation",
  description = "Configuration of Machine Translation services.",
  displayName = "Machine Translation",
)
open class MachineTranslationProperties(
  @NestedConfigurationProperty
  var google: GoogleMachineTranslationProperties = GoogleMachineTranslationProperties(),
  @NestedConfigurationProperty
  var aws: AwsMachineTranslationProperties = AwsMachineTranslationProperties(),
  @NestedConfigurationProperty
  var deepl: DeeplMachineTranslationProperties = DeeplMachineTranslationProperties(),
  @NestedConfigurationProperty
  var azure: AzureCognitiveTranslationProperties = AzureCognitiveTranslationProperties(),
  @NestedConfigurationProperty
  var baidu: BaiduMachineTranslationProperties = BaiduMachineTranslationProperties(),
  @DocProperty(
    description =
      "Amount of machine translations users of the Free tier can request per month. " +
        "Used by Tolgee Cloud, see [pricing](https://tolgee.io/pricing). " +
        "Set to `-1` to disable credit-based limitation.",
  )
  var freeCreditsAmount: Long = -1,
)
