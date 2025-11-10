package io.tolgee.ee.service.prompt

import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.service.LlmPropertiesService
import org.springframework.stereotype.Component

@Component
class DefaultPromptHelper(
  private val llmPropertiesService: LlmPropertiesService,
) {
  fun getDefaultPrompt(): PromptDto {
    return PromptDto(
      name = "",
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

        {{fragment.glossary}}

        {{fragment.keyName}}

        {{fragment.keyDescription}}

        {{fragment.translationInfo}}

        {{fragment.translateJson}}
        """.trimIndent(),
      providerName = llmPropertiesService.getProviders().getOrNull(0)?.name ?: "default",
    )
  }
}
