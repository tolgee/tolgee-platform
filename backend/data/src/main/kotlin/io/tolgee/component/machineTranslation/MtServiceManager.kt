package io.tolgee.component.machineTranslation

import io.tolgee.configuration.tolgee.InternalProperties
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
  val applicationContext: ApplicationContext,
  val internalProperties: InternalProperties
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
      service.key to let {
        if (!internalProperties.fakeMtProviders)
          service.value.translate(text, sourceLanguageTag, targetLanguageTag)
        else "$text translated with ${service.key.name} from $sourceLanguageTag to $targetLanguageTag"
      }
    }.toMap()
  }

  /**
   * Translates a text using All services
   */
  fun translate(
    text: String,
    sourceLanguageTag: String,
    targetLanguageTag: String,
    service: MtServiceType
  ): String? {
    val provider = service.getProvider()
    return if (!internalProperties.fakeMtProviders)
      provider.translate(text, sourceLanguageTag, targetLanguageTag)
    else "$text translated with ${service.name} from $sourceLanguageTag to $targetLanguageTag"
  }

  /**
   * Returns sum price of all translations
   */
  fun calculatePrice(text: String, service: MtServiceType): Int {
    return service.getProvider().calculatePrice(text)
  }

  /**
   * Returns sum price of all translations
   */
  fun calculatePriceAll(text: String, services: List<MtServiceType>): Int {
    return services.getProviders().values.sumOf { it.calculatePrice(text) }
  }

  private fun List<MtServiceType>.getProviders():
    Map<MtServiceType, MtValueProvider> {
    return this.associateWith { it.getProvider() }
  }

  private fun MtServiceType.getProvider(): MtValueProvider {
    return applicationContext.getBean(this.providerClass)
  }
}
