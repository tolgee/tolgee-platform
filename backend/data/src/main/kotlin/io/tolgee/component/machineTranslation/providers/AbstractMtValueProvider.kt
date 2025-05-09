package io.tolgee.component.machineTranslation.providers

import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.TranslationApiRateLimitException
import io.tolgee.component.machineTranslation.metadata.MtMetadata
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException

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

    try {
      return translateViaProvider(
        params.apply {
          sourceLanguageTag = suitableSourceTag
          targetLanguageTag = suitableTargetTag
        },
      )
    } catch (e: TranslationApiRateLimitException) {
      if (params.isBatch) {
        throw e
      } else {
        throw BadRequestException(Message.LLM_RATE_LIMITED)
      }
    }
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
