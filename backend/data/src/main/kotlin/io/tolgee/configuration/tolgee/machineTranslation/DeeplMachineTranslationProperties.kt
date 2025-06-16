package io.tolgee.configuration.tolgee.machineTranslation

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.machine-translation.deepl")
@DocProperty(
  description = "See [DeepL's](https://www.deepl.com/) page for more information and applicable pricing.",
  displayName = "DeepL",
)
open class DeeplMachineTranslationProperties(
  @DocProperty(description = "Whether DeepL-powered machine translation is enabled.")
  override var defaultEnabled: Boolean = true,
  @DocProperty(description = "Whether to use DeepL as a primary translation engine.")
  override var defaultPrimary: Boolean = false,
  @DocProperty(description = "DeepL auth key. Both key types (commercial and free) are supported.")
  var authKey: String? = null,
  @DocProperty(description = "DeepL parameters which should be set for deepl api usage (Optional)")
  var optionalParameters: Map<String, String>? = null,
) : MachineTranslationServiceProperties
