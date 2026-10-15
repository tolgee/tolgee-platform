package io.tolgee.ee.service.prompt

import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.model.enums.PromptType
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

  fun getDefaultReviewPrompt(): PromptDto {
    return PromptDto(
      name = "",
      template =
        """
        {{fragment.reviewIntro}}

        {{fragment.projectDescription}}

        {{fragment.languageNotes}}

        {{fragment.screenshot}}

        {{fragment.relatedKeys}}

        {{fragment.translationMemory}}

        {{fragment.glossary}}

        {{fragment.keyName}}

        {{fragment.keyDescription}}

        {{fragment.charLimit}}

        {{fragment.reviewCriteria}}

        {{fragment.reviewScoring}}

        {{fragment.reviewInfo}}

        {{fragment.reviewJson}}
        """.trimIndent(),
      providerName = LlmPropertiesService.DEFAULT_PROVIDER_ALIAS,
      type = PromptType.REVIEW,
    )
  }

  fun getDefaultPrompt(type: PromptType): PromptDto =
    when (type) {
      PromptType.TRANSLATE -> getDefaultPrompt()
      PromptType.REVIEW -> getDefaultReviewPrompt()
    }
}
