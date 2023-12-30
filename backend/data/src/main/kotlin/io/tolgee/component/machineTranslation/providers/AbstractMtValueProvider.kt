package io.tolgee.component.machineTranslation.providers

import io.tolgee.component.machineTranslation.MtValueProvider

abstract class AbstractMtValueProvider : MtValueProvider {
  private val String.toSuitableTag: String?
    get() = getSuitableTag(this)

  fun isFormalitySupported(tag: String): Boolean {
    val suitableTag = getSuitableTag(tag) ?: return false
    return formalitySupportingLanguages?.contains(suitableTag) ?: false
  }

  override fun translate(params: ProviderTranslateParams): MtValueProvider.MtResult {
    val suitableSourceTag = params.sourceLanguageTag.toSuitableTag
    val suitableTargetTag = params.targetLanguageTag.toSuitableTag

    if (suitableSourceTag.isNullOrEmpty() || suitableTargetTag.isNullOrEmpty()) {
      return MtValueProvider.MtResult(
        null,
        0,
      )
    }

    if (suitableSourceTag == suitableTargetTag) {
      return MtValueProvider.MtResult(
        params.text,
        0,
      )
    }

    return translateViaProvider(
      params.apply {
        sourceLanguageTag = suitableSourceTag
        targetLanguageTag = suitableTargetTag
      },
    )
  }

  /**
   * Translates the text via provider.
   * All inputs are already checked.
   */
  protected abstract fun translateViaProvider(params: ProviderTranslateParams): MtValueProvider.MtResult
}
