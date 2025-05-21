package io.tolgee.constants

import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.providers.*
import io.tolgee.configuration.tolgee.machineTranslation.*

enum class MtServiceType(
  val propertyClass: Class<out MachineTranslationServiceProperties>?,
  val providerClass: Class<out MtValueProvider>,
  val usesMetadata: Boolean = false,
  val supportsPlurals: Boolean = false,
  val order: Int = 0,
) {
  GOOGLE(
    propertyClass = GoogleMachineTranslationProperties::class.java,
    providerClass = GoogleTranslationProvider::class.java,
    order = 1,
  ),
  AWS(
    propertyClass = AwsMachineTranslationProperties::class.java,
    providerClass = AwsMtValueProvider::class.java,
    order = 2,
  ),
  DEEPL(
    propertyClass = DeeplMachineTranslationProperties::class.java,
    providerClass = DeeplTranslationProvider::class.java,
    order = 3,
  ),
  AZURE(
    propertyClass = AzureCognitiveTranslationProperties::class.java,
    providerClass = AzureCognitiveTranslationProvider::class.java,
    order = 4,
  ),
  BAIDU(
    propertyClass = BaiduMachineTranslationProperties::class.java,
    providerClass = BaiduTranslationProvider::class.java,
    order = 5,
  ),
  PROMPT(
    propertyClass = null,
    providerClass = LLMTranslationProvider::class.java,
    usesMetadata = true,
    order = -1,
    supportsPlurals = true,
  ),
}
