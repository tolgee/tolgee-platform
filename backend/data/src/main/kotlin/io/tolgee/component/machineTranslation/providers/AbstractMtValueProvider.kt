package io.tolgee.component.machineTranslation.providers

import io.tolgee.component.machineTranslation.LanguageTagConvertor
import io.tolgee.component.machineTranslation.MtValueProvider

abstract class AbstractMtValueProvider : MtValueProvider {
  abstract val supportedLanguages: Array<String>?

  private val String.toSuitableTag: String?
    get() {
      if (supportedLanguages.isNullOrEmpty()) {
        return this
      }
      return LanguageTagConvertor.findSuitableTag(supportedLanguages!!, this)
    }

  override fun translate(params: ProviderTranslateParams): String? {
    val suitableSourceTag = params.sourceLanguageTag.toSuitableTag
    val suitableTargetTag = params.targetLanguageTag.toSuitableTag

    if (suitableSourceTag.isNullOrEmpty() || suitableTargetTag.isNullOrEmpty()) {
      return null
    }

    if (suitableSourceTag == suitableTargetTag) {
      return params.text
    }

    return translateViaProvider(
      params.apply {
        sourceLanguageTag = suitableSourceTag
        targetLanguageTag = suitableTargetTag
      }
    )
  }

  override fun calculatePrice(params: ProviderTranslateParams): Int {
    val suitableSourceTag = params.sourceLanguageTag.toSuitableTag
    val suitableTargetTag = params.targetLanguageTag.toSuitableTag

    if (suitableSourceTag.isNullOrEmpty() ||
      suitableTargetTag.isNullOrEmpty() ||
      suitableSourceTag == suitableTargetTag
    ) {
      return 0
    }

    return calculateProviderPrice(params.text)
  }

  /**
   * Translates the text via provider.
   * All inputs are already checked.
   */
  protected abstract fun translateViaProvider(params: ProviderTranslateParams): String?

  /**
   * Calculates provider's credit price.
   * All inputs are already checked.
   */
  protected abstract fun calculateProviderPrice(text: String): Int
}
