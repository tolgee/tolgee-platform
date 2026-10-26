package io.tolgee.ee.api.v2.hateoas.assemblers

import io.tolgee.ee.api.v2.controllers.PromptController
import io.tolgee.ee.service.aiReview.AiReviewRuleService
import io.tolgee.hateoas.prompt.PromptModel
import io.tolgee.model.Prompt
import io.tolgee.model.enums.PromptType
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class PromptModelAssembler(
  private val aiReviewRuleService: AiReviewRuleService,
) : RepresentationModelAssemblerSupport<Prompt, PromptModel>(
    PromptController::class.java,
    PromptModel::class.java,
  ) {
  override fun toModel(entity: Prompt): PromptModel {
    return PromptModel(
      id = entity.id,
      name = entity.name,
      template = entity.template,
      projectId = entity.project.id,
      providerName = entity.providerName,
      basicPromptOptions = entity.basicPromptOptions?.toList(),
      type = entity.type,
      reviewCriteria = entity.reviewCriteria,
      revision = entity.revision,
      activeRuleCount = activeRuleCount(entity),
    )
  }

  private fun activeRuleCount(prompt: Prompt): Long {
    if (prompt.type != PromptType.REVIEW) return 0
    return aiReviewRuleService.countActiveRulesUsingPrompt(prompt.project.id, prompt.id)
  }
}
