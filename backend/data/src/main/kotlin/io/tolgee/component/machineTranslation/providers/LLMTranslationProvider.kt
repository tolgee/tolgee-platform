package io.tolgee.component.machineTranslation.providers

import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.dtos.request.prompt.PromptRunDto
import io.tolgee.service.PromptService
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class LLMTranslationProvider(private val promptService: PromptService) : AbstractMtValueProvider() {
  override val isEnabled: Boolean get() = true

  override fun translateViaProvider(params: ProviderTranslateParams): MtValueProvider.MtResult {
    if (params.promptId == null) {
      throw Error("Prompt ID is required")
    }
    if (params.keyId == null) {
      throw Error("Key ID is required")
    }
    val prompt = promptService.findPrompt(params.projectId, params.promptId!!)
    return promptService.translateViaPrompt(
      params.projectId,
      PromptRunDto(
        template = prompt.template,
        provider = prompt.providerName,
        targetLanguageId = params.targetLanguageId,
        keyId = params.keyId!!,
      )
    )
  }

  override fun isLanguageSupported(tag: String): Boolean = true

  // empty array meaning all is supported
  override val supportedLanguages = arrayOf<String>()
  override val formalitySupportingLanguages: Array<String>? = null
}

