package io.tolgee.ee.component

import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.providers.LLMTranslationProvider
import io.tolgee.component.machineTranslation.providers.ProviderTranslateParams
import io.tolgee.configuration.tolgee.machineTranslation.LLMProperties
import io.tolgee.dtos.request.prompt.PromptRunDto
import io.tolgee.ee.service.prompt.PromptServiceEeImpl
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Primary
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class LLMTranslationProviderEeImpl(
  private val promptService: PromptServiceEeImpl,
  private val llmProperties: LLMProperties,
) : LLMTranslationProvider() {
  override val isEnabled: Boolean get() = llmProperties.enabled

  override fun translateViaProvider(params: ProviderTranslateParams): MtValueProvider.MtResult {
    if (params.keyId == null) {
      throw Error("Key ID is required")
    }
    val prompt = promptService.findPromptOrDefaultDto(params.projectId, params.promptId)
    return promptService.translateViaPrompt(
      params.projectId,
      PromptRunDto(
        template = prompt.template,
        provider = prompt.providerName,
        targetLanguageId = params.targetLanguageId,
        keyId = params.keyId!!,
      ),
    )
  }

  override fun isLanguageSupported(tag: String): Boolean = true

  // empty array meaning all is supported
  override val supportedLanguages = arrayOf<String>()
  override val formalitySupportingLanguages: Array<String>? = null
}
