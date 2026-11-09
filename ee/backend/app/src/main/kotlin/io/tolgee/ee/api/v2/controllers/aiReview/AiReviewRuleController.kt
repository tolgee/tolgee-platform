package io.tolgee.ee.api.v2.controllers.aiReview

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Feature
import io.tolgee.ee.api.v2.hateoas.assemblers.aiReview.AiReviewRuleModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.aiReview.AiReviewRuleModel
import io.tolgee.ee.data.aiReview.AiReviewRuleEnabledRequest
import io.tolgee.ee.data.aiReview.AiReviewRuleRequest
import io.tolgee.ee.service.aiReview.AiReviewRuleService
import io.tolgee.ee.service.aiReview.AiReviewRuleStatsService
import io.tolgee.ee.service.aiReview.AiReviewRuleStatsService.AiReviewRuleCounts
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiUnstableOperationExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.ReadOnlyOperation
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresProjectPermissions
import jakarta.validation.Valid
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/v2/projects/{projectId:[0-9]+}/ai-review-rules")
@OpenApiUnstableOperationExtension
@Tag(name = "AI review rules")
class AiReviewRuleController(
  private val projectHolder: ProjectHolder,
  private val aiReviewRuleService: AiReviewRuleService,
  private val aiReviewRuleModelAssembler: AiReviewRuleModelAssembler,
  private val aiReviewRuleStatsService: AiReviewRuleStatsService,
) {
  @GetMapping("")
  @Operation(summary = "List AI review rules of the project")
  @ReadOnlyOperation
  @RequiresProjectPermissions([Scope.PROMPTS_VIEW])
  @AllowApiAccess
  @RequiresFeatures(Feature.AI_REVIEW)
  fun listAiReviewRules(): CollectionModel<AiReviewRuleModel> {
    val projectId = projectHolder.project.id
    val counts = aiReviewRuleStatsService.getCounts(projectId)
    return CollectionModel.of(
      aiReviewRuleService.list(projectId).map {
        aiReviewRuleModelAssembler.toModel(it, counts[it.id] ?: AiReviewRuleCounts(ruleId = it.id))
      },
    )
  }

  @GetMapping("/{ruleId:[0-9]+}")
  @Operation(summary = "Get an AI review rule")
  @ReadOnlyOperation
  @RequiresProjectPermissions([Scope.PROMPTS_VIEW])
  @AllowApiAccess
  @RequiresFeatures(Feature.AI_REVIEW)
  fun getAiReviewRule(
    @PathVariable ruleId: Long,
  ): AiReviewRuleModel {
    val rule = aiReviewRuleService.get(projectHolder.project.id, ruleId)
    val counts = aiReviewRuleStatsService.getCounts(projectHolder.project.id)[rule.id] ?: AiReviewRuleCounts(rule.id)
    return aiReviewRuleModelAssembler.toModel(rule, counts)
  }

  @PostMapping("")
  @Operation(summary = "Create an AI review rule")
  @RequestActivity(ActivityType.AI_REVIEW_RULE_CREATE)
  @RequiresProjectPermissions([Scope.PROMPTS_EDIT])
  @AllowApiAccess
  @RequiresFeatures(Feature.AI_REVIEW)
  fun createAiReviewRule(
    @RequestBody @Valid
    request: AiReviewRuleRequest,
  ): AiReviewRuleModel = toModelWithoutCounts(aiReviewRuleService.create(projectHolder.project.id, request))

  @PutMapping("/{ruleId:[0-9]+}")
  @Operation(summary = "Update an AI review rule")
  @RequestActivity(ActivityType.AI_REVIEW_RULE_UPDATE)
  @RequiresProjectPermissions([Scope.PROMPTS_EDIT])
  @AllowApiAccess
  @RequiresFeatures(Feature.AI_REVIEW)
  fun updateAiReviewRule(
    @PathVariable ruleId: Long,
    @RequestBody @Valid
    request: AiReviewRuleRequest,
  ): AiReviewRuleModel = toModelWithoutCounts(aiReviewRuleService.update(projectHolder.project.id, ruleId, request))

  @PutMapping("/{ruleId:[0-9]+}/enabled")
  @Operation(summary = "Enable or disable an AI review rule")
  @RequestActivity(ActivityType.AI_REVIEW_RULE_UPDATE)
  @RequiresProjectPermissions([Scope.PROMPTS_EDIT])
  @AllowApiAccess
  @RequiresFeatures(Feature.AI_REVIEW)
  fun setAiReviewRuleEnabled(
    @PathVariable ruleId: Long,
    @RequestBody @Valid
    request: AiReviewRuleEnabledRequest,
  ): AiReviewRuleModel =
    toModelWithoutCounts(aiReviewRuleService.setEnabled(projectHolder.project.id, ruleId, request.enabled))

  @DeleteMapping("/{ruleId:[0-9]+}")
  @Operation(summary = "Delete an AI review rule")
  @RequestActivity(ActivityType.AI_REVIEW_RULE_DELETE)
  @RequiresProjectPermissions([Scope.PROMPTS_EDIT])
  @AllowApiAccess
  @RequiresFeatures(Feature.AI_REVIEW)
  fun deleteAiReviewRule(
    @PathVariable ruleId: Long,
  ) {
    aiReviewRuleService.delete(projectHolder.project.id, ruleId)
  }

  private fun toModelWithoutCounts(rule: AiReviewRule): AiReviewRuleModel =
    aiReviewRuleModelAssembler.toModel(rule, null)
}
