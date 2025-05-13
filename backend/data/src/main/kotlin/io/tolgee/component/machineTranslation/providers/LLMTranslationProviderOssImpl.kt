package io.tolgee.component.machineTranslation.providers

import io.tolgee.component.machineTranslation.MtValueProvider
import org.springframework.stereotype.Component

@Component
class LLMTranslationProviderOssImpl : LLMTranslationProvider() {
  override val isEnabled: Boolean
    get() = false

  override fun translateViaProvider(params: ProviderTranslateParams): MtValueProvider.MtResult {
    TODO("Not yet implemented")
  }

  override val supportedLanguages: Array<String>?
    get() = null
  override val formalitySupportingLanguages: Array<String>?
    get() = null
}
