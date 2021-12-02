package io.tolgee.component.machineTranslation

import io.tolgee.constants.MachineTranslationServiceType
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component


/**
 * Manages machine translation third-party services.
 *
 * Enables their registering and translating with using them
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class MachineTranslateServiceManager(
  val applicationContext: ApplicationContext
) {
  private val providers by lazy {
    MachineTranslationServiceType.values().associateWith { applicationContext.getBean(it.providerClass) }
  }

  val serviceCount: Int
    get() = providers.size

  /**
   * Translates a text using All services
   */
  fun translateUsingAll(text: String,
                        sourceLanguageTag: String,
                        targetLanguageTag: String,
                        services: List<MachineTranslationServiceType>)
    : Map<MachineTranslationServiceType, String?> {
    return services.getProviders().map { service ->
      service.key to service.value.translate(text, sourceLanguageTag, targetLanguageTag)
    }.toMap()
  }

  /**
   * Returns sum price of all translations
   */
  fun calculatePriceAll(text: String, services: List<MachineTranslationServiceType>): Int {
    return services.getProviders().values.sumOf { it.calculatePrice(text) }
  }

  private fun List<MachineTranslationServiceType>.getProviders():
    Map<MachineTranslationServiceType, MachineTranslationValueProvider> {
    return this.associateWith { applicationContext.getBean(it.providerClass) }
  }


}

