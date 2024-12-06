package io.tolgee.component.machineTranslation.providers.tolgee

import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.providers.ProviderTranslateParams

interface TolgeeTranslationProvider : MtValueProvider {
  override val isEnabled: Boolean

  // empty array meaning all is supported
  override val supportedLanguages: Array<String>
  override val formalitySupportingLanguages: Array<String>

  fun translateViaProvider(params: ProviderTranslateParams): MtValueProvider.MtResult

  override fun isLanguageSupported(tag: String): Boolean

  override fun isLanguageFormalitySupported(tag: String): Boolean
}
