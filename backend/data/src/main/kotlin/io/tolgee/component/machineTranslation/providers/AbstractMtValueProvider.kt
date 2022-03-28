package io.tolgee.component.machineTranslation.providers

import io.tolgee.component.machineTranslation.LanguageTagConvertor
import io.tolgee.component.machineTranslation.MtValueProvider

abstract class AbstractMtValueProvider : MtValueProvider {
  abstract val supportedLanguages: Array<String>

  private val String.toSuitableTag: String?
    get() {
      return LanguageTagConvertor.findSuitableTag(supportedLanguages, this)
    }

  override fun translate(text: String, sourceLanguageTag: String, targetLanguageTag: String): String? {
    val suitableSourceTag = sourceLanguageTag.toSuitableTag
    val suitableTargetTag = targetLanguageTag.toSuitableTag

    if (suitableSourceTag.isNullOrEmpty() || suitableTargetTag.isNullOrEmpty()) {
      return null
    }

    if (suitableSourceTag == suitableTargetTag) {
      return text
    }

    return translateViaProvider(text, suitableSourceTag, suitableTargetTag)
  }

  override fun calculatePrice(text: String, sourceLanguageTag: String, targetLanguageTag: String): Int {
    val suitableSourceTag = sourceLanguageTag.toSuitableTag
    val suitableTargetTag = targetLanguageTag.toSuitableTag

    if (suitableSourceTag.isNullOrEmpty() ||
      suitableTargetTag.isNullOrEmpty() ||
      suitableSourceTag == suitableTargetTag
    ) {
      return 0
    }

    return calculateProviderPrice(text)
  }

  /**
   * Translates the text via provider.
   * All inputs are already checked.
   */
  protected abstract fun translateViaProvider(text: String, sourceTag: String, targetTag: String): String?

  /**
   * Calculates provider's credit price.
   * All inputs are already checked.
   */
  protected abstract fun calculateProviderPrice(text: String): Int
}
