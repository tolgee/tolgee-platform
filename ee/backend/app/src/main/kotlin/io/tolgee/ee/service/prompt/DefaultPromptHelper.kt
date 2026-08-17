package io.tolgee.ee.service.prompt

import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.service.LlmPropertiesService
import org.springframework.stereotype.Component

@Component
class DefaultPromptHelper {
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

        {{fragment.charLimit}}

        {{fragment.translationInfo}}

        {{fragment.translateJson}}
        """.trimIndent(),
      providerName = LlmPropertiesService.DEFAULT_PROVIDER_ALIAS,
    )
  }
}
