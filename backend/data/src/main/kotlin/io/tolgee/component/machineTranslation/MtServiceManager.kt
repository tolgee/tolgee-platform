package io.tolgee.component.machineTranslation

import io.sentry.Sentry
import io.tolgee.component.machineTranslation.providers.ProviderTranslateParams
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.constants.Caches
import io.tolgee.constants.MtServiceType
import io.tolgee.exceptions.LanguageNotSupportedException
import io.tolgee.model.mtServiceConfig.Formality
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

  fun translate(params: TranslationParams): TranslateResult {
    val provider = params.serviceInfo.serviceType.getProvider()
    validate(provider, params)

    val supportsFormality = provider.isLanguageFormalitySupported(params.targetLanguageTag)

    val translateParams =
      ProviderTranslateParams(
        params.text,
        params.textRaw,
        params.keyName,
        params.sourceLanguageTag,
        params.targetLanguageTag,
        params.metadata,
        if (supportsFormality) params.serviceInfo.formality else null,
        params.isBatch,
        pluralFormExamples = params.pluralFormExamples,
        pluralForms = params.pluralForms,
      )

    if (internalProperties.fakeMtProviders) {
      logger.debug("Fake MT provider is enabled")
      return getFaked(translateParams, params.serviceInfo.serviceType)
    }

    if (params.textRaw.isBlank()) {
      return TranslateResult(
        translatedText = null,
        contextDescription = null,
        actualPrice = 0,
        usedService = params.serviceInfo.serviceType,
        baseBlank = true,
      )
    }

    val foundInCache = findInCache(translateParams, params.serviceInfo.serviceType)
    if (foundInCache != null) {
      return foundInCache
    }

    return try {
      val translated =
        provider.translate(translateParams)

      val translateResult =
        TranslateResult(
          translated.translated,
          translated.contextDescription,
          translated.price,
          params.serviceInfo.serviceType,
          params.textRaw.isBlank(),
        )

      translateParams.cacheResult(translateResult, params.serviceInfo.serviceType)

      return translateResult
    } catch (e: Exception) {
      handleSilentFail(params, e)
      TranslateResult(
        translatedText = null,
        contextDescription = null,
        actualPrice = 0,
        usedService = params.serviceInfo.serviceType,
        baseBlank = params.textRaw.isBlank(),
        exception = e,
      )
    }
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

  private fun getFaked(
    params: ProviderTranslateParams,
    serviceType: MtServiceType,
  ): TranslateResult {
    val formalityIndicator =
      if ((params.formality ?: Formality.DEFAULT) !== Formality.DEFAULT) {
        "${params.formality} "
      } else {
        ""
      }
    val fakedText =
      "${params.text} translated ${formalityIndicator}with ${serviceType.name} " +
        "from ${params.sourceLanguageTag} to ${params.targetLanguageTag}"

    return TranslateResult(
      translatedText = fakedText,
      contextDescription = null,
      actualPrice = params.text.length * 100,
      usedService = serviceType,
      baseBlank = params.textRaw.isEmpty(),
    )
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
