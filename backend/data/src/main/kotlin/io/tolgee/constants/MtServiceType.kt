package io.tolgee.constants

import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.providers.AwsMtValueProvider
import io.tolgee.component.machineTranslation.providers.GoogleTranslationProvider
import io.tolgee.configuration.tolgee.machineTranslation.AwsMachineTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.GoogleMachineTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.MachineTranslationServiceProperties

enum class MtServiceType(
  val propertyClass: Class<out MachineTranslationServiceProperties>,
  val providerClass: Class<out MtValueProvider>
) {
  GOOGLE(GoogleMachineTranslationProperties::class.java, GoogleTranslationProvider::class.java),
  AWS(AwsMachineTranslationProperties::class.java, AwsMtValueProvider::class.java);
}
