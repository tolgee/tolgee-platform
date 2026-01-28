package io.tolgee.configuration.tolgee.machineTranslation

import io.tolgee.configuration.annotations.DocProperty

@DocProperty(
  prefix = "tolgee.machine-translation.azurecognitive",
  description =
    "See " +
      "[Azure Cognitive Translation]" +
      "(https://azure.microsoft.com/en-us/services/cognitive-services/translator-text-api/)" +
      " page for more information and applicable pricing.",
  displayName = "Azure Cognitive Translation",
)
open class AzureCognitiveTranslationProperties(
  @DocProperty(description = "Whether Azure Cognitive Translation is enabled.")
  override var defaultEnabled: Boolean = true,
  @DocProperty(description = "Whether to use Azure Cognitive Translation as a primary translation engine.")
  override var defaultPrimary: Boolean = false,
  @DocProperty(description = "Azure Cognitive Translation auth key.")
  var authKey: String? = null,
  @DocProperty(description = "Azure Cognitive Translation region.")
  var region: String? = null,
) : MachineTranslationServiceProperties
