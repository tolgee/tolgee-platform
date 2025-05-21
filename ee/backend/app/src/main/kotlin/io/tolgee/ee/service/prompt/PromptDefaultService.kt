package io.tolgee.ee.service.prompt

import io.tolgee.configuration.tolgee.machineTranslation.LLMProperties
import io.tolgee.dtos.request.prompt.PromptDto
import org.springframework.stereotype.Service

@Service
class PromptDefaultService(private val llmProperties: LLMProperties) {
  fun getDefaultPrompt(): PromptDto {
    return PromptDto(
      name = "Default",
      template =
        """
        {{fragment.intro}}

        {{fragment.styleInfo}}

        {{fragment.projectDescription}}
        
        {{fragment.languageNotes}}

        {{fragment.icuInfo}}

        {{fragment.screenshot}}

        {{fragment.relatedKeys}}

        {{fragment.translationMemory}}

        {{fragment.keyName}}

        {{fragment.keyDescription}}

        {{fragment.translationInfo}}

        {{fragment.translateJson}}
        """.trimIndent(),
      providerName = llmProperties.providers.getOrNull(0)?.name ?: "default",
    )
  }
}
