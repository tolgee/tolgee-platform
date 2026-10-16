package io.tolgee.ee.api.v2.controllers.aiReview

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Feature
import io.tolgee.constants.FeatureFlag
import io.tolgee.ee.api.v2.hateoas.model.aiReview.AiReviewSettingsModel
import io.tolgee.ee.data.aiReview.AiReviewSettingsRequest
import io.tolgee.ee.service.aiReview.AiReviewSettingsService
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiUnstableOperationExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.ReadOnlyOperation
import io.tolgee.security.authorization.RequiresFeatureFlag
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresProjectPermissions
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:[0-9]+}/ai-review/settings", "/v2/projects/ai-review/settings"])
@Tag(name = "AI review")
@OpenApiUnstableOperationExtension
@RequiresFeatureFlag(FeatureFlag.AI_REVIEW)
class AiReviewSettingsController(
  private val projectHolder: ProjectHolder,
  private val aiReviewSettingsService: AiReviewSettingsService,
) {
  @GetMapping("")
  @Operation(summary = "Get AI review settings of the project", operationId = "getAiReviewSettings")
  @ReadOnlyOperation
  @RequiresProjectPermissions([Scope.PROMPTS_VIEW])
  @AllowApiAccess
  @RequiresFeatures(Feature.AI_REVIEW)
  fun get(): AiReviewSettingsModel = AiReviewSettingsModel.of(aiReviewSettingsService.get(projectHolder.project.id))

  @PutMapping("")
  @Operation(
    summary = "Update AI review settings of the project",
    operationId = "updateAiReviewSettings",
    description =
      "Sets what happens to existing translations when their source text changes. " +
        "Re-translating needs machine translation enabled in the auto translation settings of the language.",
  )
  @RequiresProjectPermissions([Scope.PROMPTS_EDIT])
  @AllowApiAccess
  @RequiresFeatures(Feature.AI_REVIEW)
  fun update(
    @RequestBody @Valid
    request: AiReviewSettingsRequest,
  ): AiReviewSettingsModel = AiReviewSettingsModel.of(aiReviewSettingsService.update(projectHolder.project.id, request))
}
