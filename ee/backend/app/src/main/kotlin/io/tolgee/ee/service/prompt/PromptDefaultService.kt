package io.tolgee.ee.service.prompt

import io.tolgee.dtos.request.prompt.PromptDto
import org.springframework.stereotype.Service

@Service
class PromptDefaultService {
  fun getDefaultPrompt(): PromptDto {
    return PromptDto(
      name = "default",
      template =
        """
        {{! Tolgee prompt is split into multiple fragments }}
        {{! Hover any variable to see its content }}
        {{fragment.intro}}

        {{fragment.styleInfo}}

        {{fragment.promptCustomization}}

        {{fragment.icuInfo}}

        {{fragment.relatedKeys}}

        {{fragment.translationMemory}}

        {{fragment.keyInfo}}

        {{fragment.translationInfo}}

        {{fragment.translateJson}}
        """.trimIndent(),
      providerName = "default",
    )
  }
}
