package io.tolgee.configuration.tolgee.machineTranslation

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.machine-translation")
@DocProperty(
  description = "Configuration of Machine Translation services.",
  displayName = "Machine Translation",
)
open class MachineTranslationProperties(
  var google: GoogleMachineTranslationProperties = GoogleMachineTranslationProperties(),
  var aws: AwsMachineTranslationProperties = AwsMachineTranslationProperties(),
  var deepl: DeeplMachineTranslationProperties = DeeplMachineTranslationProperties(),
  var azure: AzureCognitiveTranslationProperties = AzureCognitiveTranslationProperties(),
  var baidu: BaiduMachineTranslationProperties = BaiduMachineTranslationProperties(),
  var tolgee: TolgeeMachineTranslationProperties = TolgeeMachineTranslationProperties(),
  var openai: OpenaiMachineTranslationProperties = OpenaiMachineTranslationProperties(),
  @DocProperty(
    description =
      "Amount of machine translations users of the Free tier can request per month. " +
        "Used by Tolgee Cloud, see [pricing](https://tolgee.io/pricing). " +
        "Set to `-1` to disable credit-based limitation.",
  )
  var freeCreditsAmount: Long = -1,
)
