package io.tolgee.ee.component

import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.metadata.MtMetadata
import io.tolgee.component.machineTranslation.providers.LlmTranslationProvider
import io.tolgee.component.machineTranslation.providers.ProviderTranslateParams
import io.tolgee.dtos.LlmParams
import io.tolgee.ee.service.prompt.DefaultPromptHelper
import io.tolgee.ee.service.prompt.PromptServiceEeImpl
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.service.LlmPropertiesService
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Primary
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class LlmTranslationProviderEeImpl(
  private val promptService: PromptServiceEeImpl,
  private val llmPropertiesService: LlmPropertiesService,
  private val defaultPromptHelper: DefaultPromptHelper,
) : LlmTranslationProvider() {
  override val isEnabled: Boolean get() = llmPropertiesService.isEnabled()

  override fun translateViaProvider(params: ProviderTranslateParams): MtValueProvider.MtResult {
    val metadata = params.metadata ?: throw Error("Metadata are required here")
    val priority = if (params.isBatch) LlmProviderPriority.LOW else LlmProviderPriority.HIGH
    val promptParams = promptService.getLlmParamsFromPrompt(metadata.prompt, metadata.keyId, priority)
    return translate(promptParams, metadata.organizationId, metadata.provider)
  }

  fun translate(
    promptParams: LlmParams,
    organizationId: Long,
    provider: String,
    attempts: List<Int>? = null,
  ): MtValueProvider.MtResult {
    val result =
      promptService.runPromptWithoutChargingCredits(
        organizationId,
        promptParams,
        provider,
        attempts,
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
        template = promptDto.template ?: defaultPromptHelper.getDefaultPrompt().template!!,
        keyId = keyId,
        targetLanguageId = targetLanguageId,
        options = promptDto.basicPromptOptions,
      )
    return MtMetadata(prompt, promptDto.providerName, keyId, organizationId)
  }

  override fun isLanguageSupported(tag: String): Boolean = true

  override fun isLanguageFormalitySupported(tag: String): Boolean = false

  // empty array meaning all is supported
  override val supportedLanguages = arrayOf<String>()
  override val formalitySupportingLanguages = null
}
