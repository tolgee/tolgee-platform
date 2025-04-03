package io.tolgee.component.machineTranslation

import io.tolgee.component.machineTranslation.providers.ProviderTranslateParams
import io.tolgee.dtos.response.prompt.PromptResponseUsageDto

interface MtValueProvider {
  val isEnabled: Boolean

  val supportedLanguages: Array<String>?

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
    val usage: PromptResponseUsageDto? = null,
  )

  val formalitySupportingLanguages: Array<String>?

  fun getSuitableTag(tag: String): String? {
    if (supportedLanguages.isNullOrEmpty()) {
      return tag
    }
    return LanguageTagConvertor.findSuitableTag(supportedLanguages!!, tag)
  }
}
