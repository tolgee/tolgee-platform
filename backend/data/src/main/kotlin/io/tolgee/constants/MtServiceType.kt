package io.tolgee.constants

import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.providers.AwsMtValueProvider
import io.tolgee.component.machineTranslation.providers.AzureCognitiveTranslationProvider
import io.tolgee.component.machineTranslation.providers.BaiduTranslationProvider
import io.tolgee.component.machineTranslation.providers.DeeplTranslationProvider
import io.tolgee.component.machineTranslation.providers.GoogleTranslationProvider
import io.tolgee.component.machineTranslation.providers.LlmTranslationProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.configuration.tolgee.machineTranslation.MachineTranslationServiceProperties

enum class MtServiceType(
  val propertiesGetter: ((TolgeeProperties) -> MachineTranslationServiceProperties),
  val providerClass: Class<out MtValueProvider>,
  val usesMetadata: Boolean = false,
  val supportsPlurals: Boolean = false,
  val order: Int = 0,
) {
  GOOGLE(
    propertiesGetter = { it.machineTranslation.google },
    providerClass = GoogleTranslationProvider::class.java,
    order = 1,
  ),
  AWS(
    propertiesGetter = { it.machineTranslation.aws },
    providerClass = AwsMtValueProvider::class.java,
    order = 2,
  ),
  DEEPL(
    propertiesGetter = { it.machineTranslation.deepl },
    providerClass = DeeplTranslationProvider::class.java,
    order = 3,
  ),
  AZURE(
    propertiesGetter = { it.machineTranslation.azure },
    providerClass = AzureCognitiveTranslationProvider::class.java,
    order = 4,
  ),
  BAIDU(
    propertiesGetter = { it.machineTranslation.baidu },
    providerClass = BaiduTranslationProvider::class.java,
    order = 5,
  ),
  PROMPT(
    propertiesGetter = { it.llm },
    providerClass = LlmTranslationProvider::class.java,
    usesMetadata = true,
    supportsPlurals = true,
    order = -1,
  ),
}
