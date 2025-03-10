package io.tolgee.ee.component

import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.metadata.MtMetadata
import io.tolgee.component.machineTranslation.providers.LLMTranslationProvider
import io.tolgee.component.machineTranslation.providers.ProviderTranslateParams
import io.tolgee.configuration.tolgee.machineTranslation.LLMProperties
import io.tolgee.ee.service.prompt.PromptDefaultService
import io.tolgee.ee.service.prompt.PromptServiceEeImpl
import io.tolgee.model.enums.LLMProviderPriority
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
  private val promptDefaultService: PromptDefaultService,
) : LLMTranslationProvider() {
  override val isEnabled: Boolean get() = llmProperties.enabled

  override fun translateViaProvider(params: ProviderTranslateParams): MtValueProvider.MtResult {
    val metadata = params.metadata ?: throw Error("Metadata are required here")
    val promptParams = promptService.getLLMParamsFromPrompt(metadata.prompt, metadata.keyId)
    val result =
      promptService.runPrompt(
        metadata.organizationId,
        params = promptParams,
        provider = metadata.provider,
        priority = if (params.isBatch) LLMProviderPriority.LOW else LLMProviderPriority.HIGH,
      )
    return promptService.getTranslationFromPromptResult(result)
  }

  override fun getMetadata(
    organizationId: Long,
    projectId: Long,
    keyId: Long?,
    targetLanguageId: Long,
    promptId: Long?,
  ): MtMetadata? {
    val promptDto = promptService.findPromptOrDefaultDto(projectId, promptId)
    if (keyId == null) {
      throw Error("Key ID is required")
    }
    val prompt =
      promptService.getPrompt(
        projectId,
        template = promptDto.template ?: promptDefaultService.getDefaultPrompt().template!!,
        keyId = keyId,
        targetLanguageId = targetLanguageId,
        provider = promptDto.providerName,
        options = promptDto.options,
      )
    return MtMetadata(prompt, promptDto.providerName, keyId, organizationId)
  }

  override fun isLanguageSupported(tag: String): Boolean = true

  override fun isLanguageFormalitySupported(tag: String): Boolean = true

  // empty array meaning all is supported
  override val supportedLanguages = arrayOf<String>()
  override val formalitySupportingLanguages = arrayOf<String>()
}
