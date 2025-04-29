package io.tolgee.component.machineTranslation

import io.sentry.Sentry
import io.tolgee.component.machineTranslation.providers.ProviderTranslateParams
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.constants.Caches
import io.tolgee.constants.MtServiceType
import io.tolgee.exceptions.LanguageNotSupportedException
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
  private val cacheManager: CacheManager,
) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  fun translate(params: TranslationParams): TranslateResult {
    val provider = params.serviceInfo.serviceType.getProvider()
    validate(provider, params)

    val translateParams = getProviderTranslateParams(provider, params)

    if (internalProperties.fakeMtProviders) {
      logger.debug("Fake MT provider is enabled")
      val fakedResult = FakedMtResultProvider(params).get()
      return getTranslateResult(fakedResult, params.serviceInfo.serviceType, params.textRaw)
    }

    if (params.textRaw.isBlank()) {
      return getBlankResult(params.serviceInfo.serviceType)
    }

    val foundInCache = findInCache(translateParams, params.serviceInfo.serviceType)
    if (foundInCache != null) {
      return foundInCache
    }

    return try {
      val translated =
        provider.translate(translateParams)

      val translateResult = getTranslateResult(translated, params.serviceInfo.serviceType, params.textRaw)

      translateParams.cacheResult(translateResult, params.serviceInfo.serviceType)

      return translateResult
    } catch (e: Exception) {
      handleSilentFail(params, e)
      getEmptyResult(params.serviceInfo.serviceType, params.textRaw.isBlank(), e)
    }
  }

  private fun getProviderTranslateParams(
    provider: MtValueProvider,
    params: TranslationParams,
  ): ProviderTranslateParams {
    val supportsFormality = provider.isLanguageFormalitySupported(params.targetLanguageTag)

    val translateParams =
      ProviderTranslateParams(
        text = params.text,
        textRaw = params.textRaw,
        keyName = params.keyName,
        sourceLanguageTag = params.sourceLanguageTag,
        targetLanguageTag = params.targetLanguageTag,
        metadata = params.metadata,
        formality = if (supportsFormality) params.serviceInfo.formality else null,
        isBatch = params.isBatch,
        pluralFormExamples = params.pluralFormExamples,
        pluralForms = params.pluralForms,
      )

    return translateParams
  }

  private fun findInCache(
    params: ProviderTranslateParams,
    serviceType: MtServiceType,
  ): TranslateResult? {
    return params.findInCacheByParams(serviceType)?.let {
      TranslateResult(
        translatedText = it.translatedText,
        contextDescription = it.contextDescription,
        actualPrice = 0,
        usedService = serviceType,
        params.textRaw.isEmpty(),
      )
    }
  }

  private fun getTranslateResult(
    mtResult: MtValueProvider.MtResult,
    serviceType: MtServiceType,
    baseTextRaw: String,
  ): TranslateResult {
    return TranslateResult(
      translatedText = mtResult.translated,
      contextDescription = mtResult.contextDescription,
      actualPrice = mtResult.price,
      usedService = serviceType,
      baseBlank = baseTextRaw.isBlank(),
    )
  }

  private fun getBlankResult(serviceType: MtServiceType): TranslateResult {
    return getEmptyResult(serviceType, true)
  }

  private fun getEmptyResult(
    serviceType: MtServiceType,
    baseBlank: Boolean,
    e: Exception? = null,
  ): TranslateResult {
    return TranslateResult(
      translatedText = null,
      contextDescription = null,
      actualPrice = 0,
      usedService = serviceType,
      baseBlank = baseBlank,
      exception = e,
    )
  }

  private fun validate(
    provider: MtValueProvider,
    params: TranslationParams,
  ) {
    if (!provider.isLanguageSupported(params.targetLanguageTag)) {
      throw LanguageNotSupportedException(params.targetLanguageTag, params.serviceInfo.serviceType)
    }
  }

  private fun handleSilentFail(
    params: TranslationParams,
    e: Exception,
  ) {
    val silentFail = !params.isBatch
    if (!silentFail) {
      throw e
    } else {
      logger.error(
        """An exception occurred while translating 
            |text "${params.text}" 
            |from ${params.sourceLanguageTag} 
            |to ${params.targetLanguageTag}"
        """.trimMargin(),
      )
      logger.error(e.stackTraceToString())
      Sentry.captureException(e)
    }
  }

  private fun ProviderTranslateParams.findInCacheByParams(serviceType: MtServiceType): TranslateResult? {
    return getCache()?.let { cache ->
      val result = cache.get(this.cacheKey(serviceType.name))?.get() as? TranslateResult
      result?.actualPrice = 0
      return result
    }
  }

  private fun ProviderTranslateParams.cacheResult(
    result: TranslateResult,
    serviceType: MtServiceType,
  ) {
    getCache()?.put(this.cacheKey(serviceType.name), result)
  }

  private fun getCache() = cacheManager.getCache(Caches.MACHINE_TRANSLATIONS)

  fun MtServiceType.getProvider(): MtValueProvider {
    return applicationContext.getBean(this.providerClass)
  }
}
