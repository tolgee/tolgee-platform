package io.tolgee.component.machineTranslation

import io.sentry.Sentry
import io.tolgee.component.machineTranslation.providers.ProviderTranslateParams
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.constants.Caches
import io.tolgee.constants.MtServiceType
import io.tolgee.exceptions.FormalityNotSupportedException
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

  private fun findInCache(params: TranslationParams): TranslateResult? {
    return params.findInCacheByParams()?.let {
      TranslateResult(
        translatedText = it.translatedText,
        contextDescription = it.contextDescription,
        actualPrice = 0,
        usedService = params.serviceInfo.serviceType,
        params.textRaw.isEmpty(),
      )
    }
  }

  fun translate(params: TranslationParams): TranslateResult {
    val provider = params.serviceInfo.serviceType.getProvider()
    validate(provider, params)

    if (internalProperties.fakeMtProviders) {
      logger.debug("Fake MT provider is enabled")
      return getFaked(params)
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

    val foundInCache = findInCache(params)
    if (foundInCache != null) {
      return foundInCache
    }

    return try {
      val translated =
        provider.translate(
          ProviderTranslateParams(
            params.text,
            params.textRaw,
            params.keyName,
            params.sourceLanguageTag,
            params.targetLanguageTag,
            params.metadata,
            params.serviceInfo.formality,
            params.isBatch,
            pluralFormExamples = params.pluralFormExamples,
            pluralForms = params.pluralForms,
          ),
        )

      val translateResult =
        TranslateResult(
          translated.translated,
          translated.contextDescription,
          translated.price,
          params.serviceInfo.serviceType,
          params.textRaw.isBlank(),
        )

      params.cacheResult(translateResult)

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

    val formality = params.serviceInfo.formality
    val requiresFormality =
      formality != null &&
        formality != Formality.DEFAULT

    if (!provider.isLanguageFormalitySupported(params.targetLanguageTag) && requiresFormality) {
      throw FormalityNotSupportedException(params.targetLanguageTag, params.serviceInfo.serviceType)
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

  private fun getFaked(params: TranslationParams): TranslateResult {
    var fakedText =
      "${params.text} translated with ${params.serviceInfo.serviceType.name} " +
        "from ${params.sourceLanguageTag} to ${params.targetLanguageTag}"
    if ((params.serviceInfo.formality ?: Formality.DEFAULT) !== Formality.DEFAULT) {
      fakedText += " ${params.serviceInfo.formality}"
    }
    return TranslateResult(
      translatedText = fakedText,
      contextDescription = null,
      actualPrice = params.text.length * 100,
      usedService = params.serviceInfo.serviceType,
      baseBlank = params.textRaw.isEmpty(),
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

  fun MtServiceType.getProvider(): MtValueProvider {
    return applicationContext.getBean(this.providerClass)
  }
}
