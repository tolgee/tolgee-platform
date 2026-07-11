package io.tolgee.component.machineTranslation.providers

import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.exceptions.NotImplementedInOss
import org.springframework.stereotype.Component

@Component
class LlmTranslationProviderOssImpl : LlmTranslationProvider() {
  override val isEnabled: Boolean
    get() = false

  override fun translateViaProvider(params: ProviderTranslateParams): MtValueProvider.MtResult {
    throw NotImplementedInOss()
  }

  override val supportedLanguages: Array<String>?
    get() = null
  override val formalitySupportingLanguages: Array<String>?
    get() = null
}
