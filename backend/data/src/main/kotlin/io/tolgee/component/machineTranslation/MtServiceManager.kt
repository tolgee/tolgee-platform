package io.tolgee.component.machineTranslation

import io.sentry.Sentry
import io.tolgee.component.machineTranslation.metadata.Metadata
import io.tolgee.component.machineTranslation.providers.ProviderTranslateParams
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.constants.Caches
import io.tolgee.constants.MtServiceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.cache.CacheManager
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
  private val applicationContext: ApplicationContext,
  private val internalProperties: InternalProperties,
  private val cacheManager: CacheManager
) {

  private val logger = LoggerFactory.getLogger(this::class.java)

  /**
   * Translates a text using All services
   */
  fun translateUsingAll(
    text: String,
    textRaw: String,
    keyName: String?,
    sourceLanguageTag: String,
    targetLanguageTag: String,
    services: Collection<MtServiceType>,
    metadata: Metadata?
  ): Map<MtServiceType, TranslateResult> {
    return runBlocking(Dispatchers.IO) {
      services.map { service ->
        async { service to translate(text, textRaw, keyName, sourceLanguageTag, targetLanguageTag, service, metadata) }
      }.awaitAll().toMap()
    }
  }

  private fun findInCache(
    params: TranslationParams
  ): TranslateResult? {
    return params.findInCacheByParams()?.let {
      TranslateResult(
        translatedText = it.translatedText,
        contextDescription = it.contextDescription,
        actualPrice = 0,
        usedService = params.serviceType
      )
    }
  }

  fun translate(params: TranslationParams): TranslateResult {
    if (internalProperties.fakeMtProviders) {
      return getFaked(params)
    }

    val foundInCache = findInCache(params)
    if (foundInCache != null) {
      return foundInCache
    }

    val translated = try {
      params.serviceType.getProvider()
        .translate(
          ProviderTranslateParams(
            params.text,
            params.textRaw,
            params.keyName,
            params.sourceLanguageTag,
            params.targetLanguageTag,
            params.metadata
          )
        )
    } catch (e: Exception) {
      logger.error(
        """An exception occurred while translating 
            |text "${params.text}" 
            |from ${params.sourceLanguageTag} 
            |to ${params.targetLanguageTag}"
        """.trimMargin()
      )
      logger.error(e.stackTraceToString())
      Sentry.captureException(e)
      null
    }

    val result = TranslateResult(
      translated?.translated,
      translated?.contextDescription,
      translated?.price ?: 0,
      params.serviceType
    )

    result?.let { params.cacheResult(it) }

    return result
  }

  fun getParams(
    text: String,
    textRaw: String,
    keyName: String?,
    sourceLanguageTag: String,
    targetLanguageTag: String,
    serviceType: MtServiceType,
    metadata: Metadata? = null
  ) = TranslationParams(
    text = text,
    textRaw = textRaw,
    sourceLanguageTag = sourceLanguageTag,
    targetLanguageTag = targetLanguageTag,
    serviceType = serviceType,
    metadata = metadata,
    keyName = keyName
  )

  private fun getFaked(
    params: TranslationParams
  ): TranslateResult {
    return TranslateResult(
      translatedText = "${params.text} translated with ${params.serviceType.name} " +
        "from ${params.sourceLanguageTag} to ${params.targetLanguageTag}",
      contextDescription = null,
      actualPrice = params.text.length * 100,
      usedService = params.serviceType
    )
  }

  private fun TranslationParams.findInCacheByParams(): TranslateResult? {
    return getCache()?.let { cache ->
      val result = cache.get(this.cacheKey)?.get() as? TranslateResult
      result?.actualPrice = 0
      return result
    }
  }

  private fun TranslationParams.cacheResult(result: TranslateResult) {
    getCache()?.put(this.cacheKey, result)
  }

  private fun getCache() = cacheManager.getCache(Caches.MACHINE_TRANSLATIONS)

  fun translate(
    text: String,
    textRaw: String,
    keyName: String?,
    sourceLanguageTag: String,
    targetLanguageTag: String,
    serviceType: MtServiceType,
    metadata: Metadata? = null
  ): TranslateResult {
    val params = getParams(text, textRaw, keyName, sourceLanguageTag, targetLanguageTag, serviceType, metadata)

    return translate(params)
  }

  /**
   * Translates a text using single service
   */
  fun translate(
    text: String,
    textRaw: String,
    keyName: String?,
    sourceLanguageTag: String,
    targetLanguageTags: List<String>,
    service: MtServiceType,
    metadata: Map<String, Metadata>? = null
  ): List<TranslateResult> {
    return if (!internalProperties.fakeMtProviders) {
      translateToMultipleTargets(
        serviceType = service,
        textRaw = textRaw,
        keyName = keyName,
        text = text,
        sourceLanguageTag = sourceLanguageTag,
        targetLanguageTags = targetLanguageTags,
        metadata = metadata
      )
    } else targetLanguageTags.map {
      getFaked(getParams(text, textRaw, keyName, sourceLanguageTag, it, service, null))
    }
  }

  private fun translateToMultipleTargets(
    serviceType: MtServiceType,
    text: String,
    textRaw: String,
    keyName: String?,
    sourceLanguageTag: String,
    targetLanguageTags: List<String>,
    metadata: Map<String, Metadata>? = null
  ): List<TranslateResult> {
    return runBlocking(Dispatchers.IO) {
      targetLanguageTags.map { targetLanguageTag ->
        async {
          translate(
            text,
            textRaw,
            keyName,
            sourceLanguageTag,
            targetLanguageTag,
            serviceType,
            metadata?.get(targetLanguageTag)
          )
        }
      }.awaitAll()
    }
  }

  fun MtServiceType.getProvider(): MtValueProvider {
    return applicationContext.getBean(this.providerClass)
  }
}
