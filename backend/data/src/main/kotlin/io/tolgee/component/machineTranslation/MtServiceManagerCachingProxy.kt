package io.tolgee.component.machineTranslation

import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.constants.Caches
import io.tolgee.constants.MtServiceType
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service

/**
 * Manages machine translation third-party services.
 *
 * Enables their registering and translating with using them
 */
@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class MtServiceManagerCachingProxy(
  val applicationContext: ApplicationContext,
  val internalProperties: InternalProperties,
  val cacheManager: CacheManager,
  val mtServiceManager: MtServiceManager
) {
  @Cacheable(
    cacheNames = [Caches.MACHINE_TRANSLATIONS],
    key = "{#serviceType.name(), #text, #sourceLanguageTag, #targetLanguageTag}",
  )
  fun translate(
    serviceType: MtServiceType,
    text: String,
    sourceLanguageTag: String,
    targetLanguageTag: String
  ): String? {
    val provider = with(mtServiceManager) {
      serviceType.getProvider()
    }
    return provider.translate(text, sourceLanguageTag, targetLanguageTag)
  }
}
