package io.tolgee.ee.component

import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.metadata.MtMetadata
import io.tolgee.component.machineTranslation.providers.LlmTranslationProvider
import io.tolgee.component.machineTranslation.providers.ProviderTranslateParams
import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import io.tolgee.dtos.LlmParams
import io.tolgee.ee.service.prompt.PromptDefaultService
import io.tolgee.ee.service.prompt.PromptServiceEeImpl
import io.tolgee.model.enums.LlmProviderPriority
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Primary
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class LlmTranslationProviderEeImpl(
  private val promptService: PromptServiceEeImpl,
  private val llmProperties: LlmProperties,
  private val promptDefaultService: PromptDefaultService,
) : LlmTranslationProvider() {
  override val isEnabled: Boolean get() = llmProperties.enabled

  override fun translateViaProvider(params: ProviderTranslateParams): MtValueProvider.MtResult {
    val metadata = params.metadata ?: throw Error("Metadata are required here")
    val promptParams = promptService.getLlmParamsFromPrompt(metadata.prompt, metadata.keyId)
    return translate(promptParams, metadata.organizationId, metadata.provider, params.isBatch)
  }

  fun translate(
    promptParams: LlmParams,
    organizationId: Long,
    provider: String,
    isBatch: Boolean,
  ): MtValueProvider.MtResult {
    val result =
      promptService.runPrompt(
        organizationId,
        params = promptParams,
        provider = provider,
        priority = if (isBatch) LlmProviderPriority.LOW else LlmProviderPriority.HIGH,
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

  override fun isLanguageFormalitySupported(tag: String): Boolean = false

  // empty array meaning all is supported
  override val supportedLanguages = arrayOf<String>()
  override val formalitySupportingLanguages = null
}
