package io.tolgee.component.machineTranslation

import io.tolgee.component.machineTranslation.metadata.MtMetadata
import io.tolgee.component.machineTranslation.providers.ProviderTranslateParams
import io.tolgee.dtos.PromptResult

interface MtValueProvider {
  val isEnabled: Boolean

  val supportedLanguages: Array<String>?

  /**
   * Tags accepted by the provider as translation **source**. Defaults to [supportedLanguages].
   *
   * Override this when a provider rejects some of its target-language tags as sources
   */
  val supportedSourceLanguages: Array<String>?
    get() = supportedLanguages

  fun isLanguageSupported(tag: String): Boolean {
    val suitableTag = getSuitableTag(tag) ?: return false
    return supportedLanguages?.contains(suitableTag) ?: true
  }

  fun isLanguageFormalitySupported(tag: String): Boolean {
    val suitableTag = getSuitableTag(tag) ?: return false
    return formalitySupportingLanguages?.contains(suitableTag) ?: false
  }

  /**
   * Translates the text using the service
   */
  fun translate(params: ProviderTranslateParams): MtResult

  data class MtResult(
    var translated: String?,
    val price: Int,
    val contextDescription: String? = null,
    val usage: PromptResult.Usage? = null,
  )

  val formalitySupportingLanguages: Array<String>?

  val supportsContext: Boolean
    get() = false

  fun getSuitableTag(tag: String): String? {
    if (supportedLanguages.isNullOrEmpty()) {
      return tag
    }
    return LanguageTagConvertor.findSuitableTag(supportedLanguages!!, tag)
  }

  fun getSuitableSourceTag(tag: String): String? {
    if (supportedSourceLanguages.isNullOrEmpty()) {
      return tag
    }
    return LanguageTagConvertor.findSuitableTag(supportedSourceLanguages!!, tag)
  }

  fun getMetadata(
    organizationId: Long,
    projectId: Long,
    keyId: Long?,
    targetLanguageId: Long,
    promptId: Long?,
  ): MtMetadata?
}
