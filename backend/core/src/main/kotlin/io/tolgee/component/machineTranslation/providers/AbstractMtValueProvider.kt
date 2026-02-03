package io.tolgee.component.machineTranslation.providers

import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.metadata.MtMetadata

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

  override fun getMetadata(
    organizationId: Long,
    projectId: Long,
    keyId: Long?,
    targetLanguageId: Long,
    promptId: Long?,
  ): MtMetadata? = null

  /**
   * Translates the text via provider.
   * All inputs are already checked.
   */
  protected abstract fun translateViaProvider(params: ProviderTranslateParams): MtValueProvider.MtResult
}
