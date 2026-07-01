package io.tolgee.configuration.tolgee.machineTranslation

import io.tolgee.configuration.annotations.DocProperty

@DocProperty(
  prefix = "tolgee.machine-translation.google",
  description =
    "See [Google Cloud Translation](https://cloud.google.com/translate) page " +
      "for more information and applicable pricing.",
  displayName = "Google Cloud Translation",
)
open class GoogleMachineTranslationProperties(
  @DocProperty(description = "Whether Google-powered machine translation is enabled.")
  override var defaultEnabled: Boolean = true,
  @DocProperty(description = "Whether to use Google Cloud Translation as a primary translation engine.")
  override var defaultPrimary: Boolean = true,
  @DocProperty(description = "Google Cloud Translation API key.")
  var apiKey: String? = null,
) : MachineTranslationServiceProperties
