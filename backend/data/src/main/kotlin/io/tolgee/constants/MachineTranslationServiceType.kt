package io.tolgee.constants

import io.tolgee.component.machineTranslation.MachineTranslationValueProvider
import io.tolgee.component.machineTranslation.providers.AwsTranslationProvider
import io.tolgee.component.machineTranslation.providers.GoogleTranslationProvider
import io.tolgee.configuration.tolgee.machineTranslation.AwsMachineTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.GoogleMachineTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.MachineTranslationServiceProperties

enum class MachineTranslationServiceType(
  val propertyClass: Class<out MachineTranslationServiceProperties>,
  val providerClass: Class<out MachineTranslationValueProvider>
) {
  GOOGLE(GoogleMachineTranslationProperties::class.java, GoogleTranslationProvider::class.java),
  AWS(AwsMachineTranslationProperties::class.java, AwsTranslationProvider::class.java);
}
