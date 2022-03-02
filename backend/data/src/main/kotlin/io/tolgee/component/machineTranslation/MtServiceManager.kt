package io.tolgee.component.machineTranslation

import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.constants.Caches
import io.tolgee.constants.MtServiceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.cache.CacheManager
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
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
  private val applicationContext: ApplicationContext,
  private val internalProperties: InternalProperties,
  private val cacheManager: CacheManager
) {

  @set:Autowired
  @set:Lazy
  lateinit var mtServiceManagerCachingProxy: MtServiceManagerCachingProxy

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
  ): Map<MtServiceType, TranslateResult> {
    return runBlocking(Dispatchers.IO) {
      services.map { service ->
        async { service to translate(text, sourceLanguageTag, targetLanguageTag, service) }
      }.awaitAll().toMap()
    }
  }

  /**
   * Translates a text using All services
   */
  fun translate(
    text: String,
    sourceLanguageTag: String,
    targetLanguageTag: String,
    serviceType: MtServiceType
  ): TranslateResult {
    val params = getParams(text, sourceLanguageTag, targetLanguageTag, serviceType)

    if (internalProperties.fakeMtProviders) {
      return getFaked(params)
    }

    return params.findInCache()?.let {
      TranslateResult(
        translatedText = it,
        actualPrice = 0,
        usedService = serviceType
      )
    } ?: let {
      val price = calculatePrice(params.text, params.serviceType)
      val result = TranslateResult(
        params.serviceType.getProvider()
          .translate(params.text, params.sourceLanguageTag, params.targetLanguageTag),
        price,
        params.serviceType
      )
      result.translatedText?.let { params.cacheResult(it) }
      return result
    }
  }

  private fun getParams(
    text: String,
    sourceLanguageTag: String,
    targetLanguageTag: String,
    serviceType: MtServiceType
  ) = TranslationParams(
    text = text,
    sourceLanguageTag = sourceLanguageTag,
    targetLanguageTag = targetLanguageTag,
    serviceType = serviceType
  )

  private fun getFaked(
    params: TranslationParams
  ): TranslateResult {
    return TranslateResult(
      "${params.text} translated with ${params.serviceType.name} " +
        "from ${params.sourceLanguageTag} to ${params.targetLanguageTag}",
      calculatePrice(params.text, params.serviceType),
      params.serviceType
    )
  }

  private fun TranslationParams.findInCache(): String? {
    return getCache()?.let { cache ->
      cache.get(this.cacheKey)?.get() as? String
    }
  }

  private fun TranslationParams.cacheResult(result: String) {
    getCache()?.put(this.cacheKey, result)
  }

  private fun getCache() = cacheManager.getCache(Caches.MACHINE_TRANSLATIONS)

  /**
   * Translates a text using All services
   */
  fun translate(
    text: String,
    sourceLanguageTag: String,
    targetLanguageTags: List<String>,
    service: MtServiceType
  ): List<TranslateResult> {
    return if (!internalProperties.fakeMtProviders)
      translateToMultipleTargets(
        serviceType = service,
        text = text,
        sourceLanguageTag = sourceLanguageTag,
        targetLanguageTags = targetLanguageTags
      )
    else targetLanguageTags.map { getFaked(getParams(text, sourceLanguageTag, it, service)) }
  }

  fun translateToMultipleTargets(
    serviceType: MtServiceType,
    text: String,
    sourceLanguageTag: String,
    targetLanguageTags: List<String>
  ): List<TranslateResult> {
    return runBlocking(Dispatchers.IO) {
      targetLanguageTags.map { targetLanguageTag ->
        async {
          translate(text, sourceLanguageTag, targetLanguageTag, serviceType)
        }
      }.awaitAll()
    }
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

  fun List<MtServiceType>.getProviders():
    Map<MtServiceType, MtValueProvider> {
    return this.associateWith { it.getProvider() }
  }

  fun MtServiceType.getProvider(): MtValueProvider {
    return applicationContext.getBean(this.providerClass)
  }
}
