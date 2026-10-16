package io.tolgee.ee.api.v2.controllers.aiReview

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Feature
import io.tolgee.constants.FeatureFlag
import io.tolgee.ee.api.v2.hateoas.assemblers.aiReview.AiReviewRuleModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.aiReview.AiReviewRetryFailedModel
import io.tolgee.ee.api.v2.hateoas.model.aiReview.AiReviewRuleBacklogModel
import io.tolgee.ee.api.v2.hateoas.model.aiReview.AiReviewRuleBacklogsModel
import io.tolgee.ee.api.v2.hateoas.model.aiReview.AiReviewRuleModel
import io.tolgee.ee.data.aiReview.AiReviewRuleRequest
import io.tolgee.ee.service.aiReview.AiReviewRuleService
import io.tolgee.ee.service.aiReview.AiReviewRunService
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiUnstableOperationExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.ReadOnlyOperation
import io.tolgee.security.authorization.RequiresFeatureFlag
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.security.SecurityService
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
@RequestMapping(value = ["/v2/projects/{projectId:[0-9]+}/ai-review/rules", "/v2/projects/ai-review/rules"])
@Tag(name = "AI review")
@OpenApiUnstableOperationExtension
@RequiresFeatureFlag(FeatureFlag.AI_REVIEW)
class AiReviewRuleController(
  private val projectHolder: ProjectHolder,
  private val aiReviewRuleService: AiReviewRuleService,
  private val aiReviewRuleModelAssembler: AiReviewRuleModelAssembler,
  private val aiReviewRunService: AiReviewRunService,
  private val securityService: SecurityService,
) {
  @GetMapping("")
  @Operation(summary = "Get AI review rules of the project", operationId = "getAiReviewRules")
  @ReadOnlyOperation
  @RequiresProjectPermissions([Scope.PROMPTS_VIEW])
  @AllowApiAccess
  @RequiresFeatures(Feature.AI_REVIEW)
  fun getAll(): CollectionModel<AiReviewRuleModel> {
    val projectId = projectHolder.project.id
    val stats = aiReviewRuleService.getStats(projectId)
    val models = aiReviewRuleService.getAll(projectId).map { aiReviewRuleModelAssembler.toModel(it, stats[it.id]) }
    return CollectionModel.of(models)
  }

  @GetMapping("/backlog")
  @Operation(
    summary = "Get AI review backlog of the rules",
    operationId = "getAiReviewRuleBacklogs",
    description =
      "Number of existing translations each enabled rule covers that have not been reviewed yet, " +
        "counted only in the languages where the caller can change the translation state.",
  )
  @ReadOnlyOperation
  @RequiresProjectPermissions([Scope.PROMPTS_VIEW])
  @AllowApiAccess
  @RequiresFeatures(Feature.AI_REVIEW)
  fun getBacklogs(): AiReviewRuleBacklogsModel {
    val projectId = projectHolder.project.id
    val backlogs =
      aiReviewRunService.getRuleBacklogs(projectHolder.project) { languageId ->
        canChangeState(projectId, languageId)
      }
    return AiReviewRuleBacklogsModel(
      backlogs = backlogs.map { (ruleId, notReviewed) -> AiReviewRuleBacklogModel(ruleId, notReviewed) },
    )
  }

  @PostMapping("")
  @Operation(summary = "Create AI review rule", operationId = "createAiReviewRule")
  @RequiresProjectPermissions([Scope.PROMPTS_EDIT])
  @AllowApiAccess
  @RequiresFeatures(Feature.AI_REVIEW)
  fun create(
    @RequestBody @Valid
    request: AiReviewRuleRequest,
  ): AiReviewRuleModel {
    val rule = aiReviewRuleService.create(projectHolder.project.id, request)
    return aiReviewRuleModelAssembler.toModel(rule, null)
  }

  @PutMapping("/{ruleId:[0-9]+}")
  @Operation(summary = "Update AI review rule", operationId = "updateAiReviewRule")
  @RequiresProjectPermissions([Scope.PROMPTS_EDIT])
  @AllowApiAccess
  @RequiresFeatures(Feature.AI_REVIEW)
  fun update(
    @PathVariable ruleId: Long,
    @RequestBody @Valid
    request: AiReviewRuleRequest,
  ): AiReviewRuleModel {
    val projectId = projectHolder.project.id
    val rule = aiReviewRuleService.update(projectId, ruleId, request)
    return aiReviewRuleModelAssembler.toModel(rule, aiReviewRuleService.getStats(projectId)[rule.id])
  }

  @PostMapping("/{ruleId:[0-9]+}/retry-failed")
  @Operation(
    summary = "Retry failed AI reviews of the rule",
    operationId = "retryFailedAiReviews",
    description = "Runs the AI review again for the rule's failed reviews of translations that are still translated.",
  )
  @RequiresProjectPermissions([Scope.PROMPTS_EDIT])
  @AllowApiAccess
  @RequiresFeatures(Feature.AI_REVIEW)
  fun retryFailed(
    @PathVariable ruleId: Long,
  ): AiReviewRetryFailedModel =
    AiReviewRetryFailedModel(queued = aiReviewRuleService.retryFailed(projectHolder.project.id, ruleId))

  @DeleteMapping("/{ruleId:[0-9]+}")
  @Operation(summary = "Delete AI review rule", operationId = "deleteAiReviewRule")
  @RequiresProjectPermissions([Scope.PROMPTS_EDIT])
  @AllowApiAccess
  @RequiresFeatures(Feature.AI_REVIEW)
  fun delete(
    @PathVariable ruleId: Long,
  ) {
    aiReviewRuleService.delete(projectHolder.project.id, ruleId)
  }

  private fun canChangeState(
    projectId: Long,
    languageId: Long,
  ): Boolean =
    try {
      securityService.checkLanguageStateChangePermission(projectId, listOf(languageId))
      true
    } catch (e: PermissionException) {
      false
    }
}
