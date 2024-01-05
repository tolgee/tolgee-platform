package io.tolgee.constants

import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.providers.AwsMtValueProvider
import io.tolgee.component.machineTranslation.providers.AzureCognitiveTranslationProvider
import io.tolgee.component.machineTranslation.providers.BaiduTranslationProvider
import io.tolgee.component.machineTranslation.providers.DeeplTranslationProvider
import io.tolgee.component.machineTranslation.providers.GoogleTranslationProvider
import io.tolgee.component.machineTranslation.providers.tolgee.TolgeeTranslationProvider
import io.tolgee.configuration.tolgee.machineTranslation.AwsMachineTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.AzureCognitiveTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.BaiduMachineTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.DeeplMachineTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.GoogleMachineTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.MachineTranslationServiceProperties
import io.tolgee.configuration.tolgee.machineTranslation.TolgeeMachineTranslationProperties

enum class MtServiceType(
  val propertyClass: Class<out MachineTranslationServiceProperties>,
  val providerClass: Class<out MtValueProvider>,
  val usesMetadata: Boolean = false,
  val order: Int = 0,
) {
  GOOGLE(GoogleMachineTranslationProperties::class.java, GoogleTranslationProvider::class.java),
  AWS(AwsMachineTranslationProperties::class.java, AwsMtValueProvider::class.java),
  DEEPL(DeeplMachineTranslationProperties::class.java, DeeplTranslationProvider::class.java),
  AZURE(AzureCognitiveTranslationProperties::class.java, AzureCognitiveTranslationProvider::class.java),
  BAIDU(BaiduMachineTranslationProperties::class.java, BaiduTranslationProvider::class.java),
  TOLGEE(TolgeeMachineTranslationProperties::class.java, TolgeeTranslationProvider::class.java, true, order = -1),
}
