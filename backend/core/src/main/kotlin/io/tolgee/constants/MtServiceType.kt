package io.tolgee.constants

import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.providers.AwsMtValueProvider
import io.tolgee.component.machineTranslation.providers.AzureCognitiveTranslationProvider
import io.tolgee.component.machineTranslation.providers.BaiduTranslationProvider
import io.tolgee.component.machineTranslation.providers.DeeplTranslationProvider
import io.tolgee.component.machineTranslation.providers.GoogleTranslationProvider
import io.tolgee.component.machineTranslation.providers.LlmTranslationProvider
import io.tolgee.configuration.tolgee.machineTranslation.AwsMachineTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.AzureCognitiveTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.BaiduMachineTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.DeeplMachineTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.GoogleMachineTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import io.tolgee.configuration.tolgee.machineTranslation.MachineTranslationServiceProperties

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
    propertyClass = LlmProperties::class.java,
    providerClass = LlmTranslationProvider::class.java,
    usesMetadata = true,
    order = -1,
    supportsPlurals = true,
  ),
}
