package io.tolgee.constants

import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.providers.AwsMtValueProvider
import io.tolgee.component.machineTranslation.providers.AzureCognitiveTranslationProvider
import io.tolgee.component.machineTranslation.providers.BaiduTranslationProvider
import io.tolgee.component.machineTranslation.providers.DeeplTranslationProvider
import io.tolgee.component.machineTranslation.providers.GoogleTranslationProvider
import io.tolgee.component.machineTranslation.providers.OpenaiTranslationProvider
import io.tolgee.component.machineTranslation.providers.tolgee.TolgeeTranslationProvider
import io.tolgee.configuration.tolgee.machineTranslation.AwsMachineTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.AzureCognitiveTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.BaiduMachineTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.DeeplMachineTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.GoogleMachineTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.MachineTranslationServiceProperties
import io.tolgee.configuration.tolgee.machineTranslation.OpenaiMachineTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.TolgeeMachineTranslationProperties

enum class MtServiceType(
  val propertyClass: Class<out MachineTranslationServiceProperties>,
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
  TOLGEE(
    propertyClass = TolgeeMachineTranslationProperties::class.java,
    providerClass = TolgeeTranslationProvider::class.java,
    usesMetadata = true,
    order = -1,
    supportsPlurals = true,
  ),
  OPENAI(
    propertyClass = OpenaiMachineTranslationProperties::class.java,
    providerClass = OpenaiTranslationProvider::class.java,
    order = 6,
  ),
}
