package io.tolgee.ee.service.prompt

import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.model.enums.PromptType
import io.tolgee.service.LlmPropertiesService

object DefaultReviewPromptHelper {
  const val NAME = "Default review prompt"

  fun get(): PromptDto =
    PromptDto(
      name = NAME,
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

        {{fragment.reviewInfo}}

        {{fragment.reviewJson}}
        """.trimIndent(),
      providerName = LlmPropertiesService.DEFAULT_PROVIDER_ALIAS,
      type = PromptType.REVIEW,
    )
}
