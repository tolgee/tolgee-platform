package io.tolgee.component.machineTranslation.providers.tolgee

import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.providers.ProviderTranslateParams
import org.springframework.stereotype.Component

@Component
class TolgeeTranslationProviderOssImpl : TolgeeTranslationProvider {
  override val isEnabled: Boolean = false

  // empty array meaning all is supported
  override val supportedLanguages: Array<String> = emptyArray()
  override val formalitySupportingLanguages: Array<String> = emptyArray()

  override fun translateViaProvider(params: ProviderTranslateParams): MtValueProvider.MtResult {
    throw IllegalStateException("Not implemented")
  }

  override fun isLanguageSupported(tag: String): Boolean {
    return false
  }

  override fun isLanguageFormalitySupported(tag: String): Boolean {
    return false
  }

  override fun translate(params: ProviderTranslateParams): MtValueProvider.MtResult {
    throw IllegalStateException("Not implemented")
  }
}
