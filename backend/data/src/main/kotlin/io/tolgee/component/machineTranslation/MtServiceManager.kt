package io.tolgee.component.machineTranslation

import io.tolgee.constants.MtServiceType
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
class MtServiceManager(
  val applicationContext: ApplicationContext
) {
  private val providers by lazy {
    MtServiceType.values().associateWith { applicationContext.getBean(it.providerClass) }
  }

  val serviceCount: Int
    get() = providers.size

  /**
   * Translates a text using All services
   */
  fun translateUsingAll(
    text: String,
    sourceLanguageTag: String,
    targetLanguageTag: String,
    services: List<MtServiceType>
  ): Map<MtServiceType, String?> {
    return services.getProviders().map { service ->
      service.key to service.value.translate(text, sourceLanguageTag, targetLanguageTag)
    }.toMap()
  }

  /**
   * Returns sum price of all translations
   */
  fun calculatePriceAll(text: String, services: List<MtServiceType>): Int {
    return services.getProviders().values.sumOf { it.calculatePrice(text) }
  }

  private fun List<MtServiceType>.getProviders():
    Map<MtServiceType, MtValueProvider> {
    return this.associateWith { applicationContext.getBean(it.providerClass) }
  }
}
