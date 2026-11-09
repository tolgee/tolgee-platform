package io.tolgee.ee.api.v2.controllers.aiReview

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Feature
import io.tolgee.ee.data.aiReview.AiReviewRunPreviewModel
import io.tolgee.ee.data.aiReview.AiReviewRunRequest
import io.tolgee.ee.service.aiReview.AiReviewRunService
import io.tolgee.hateoas.batch.BatchJobModel
import io.tolgee.hateoas.batch.BatchJobModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.model.views.BatchJobView
import io.tolgee.openApiDocs.OpenApiUnstableOperationExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.ReadOnlyOperation
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresProjectPermissions
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/v2/projects/{projectId:[0-9]+}/ai-review")
@Tag(name = "AI review runs")
@OpenApiUnstableOperationExtension
class AiReviewRunController(
  private val aiReviewRunService: AiReviewRunService,
  private val batchJobModelAssembler: BatchJobModelAssembler,
  private val projectHolder: ProjectHolder,
  private val authenticationFacade: AuthenticationFacade,
) {
  @PostMapping("/run/preview")
  @Operation(summary = "Preview an AI review run")
  @RequiresProjectPermissions([Scope.BATCH_MACHINE_TRANSLATE])
  @RequiresFeatures(Feature.AI_REVIEW)
  @AllowApiAccess
  @ReadOnlyOperation
  fun previewAiReviewRun(
    @RequestBody @Valid request: AiReviewRunRequest,
  ): AiReviewRunPreviewModel = aiReviewRunService.preview(projectHolder.project.id, request)

  @PostMapping("/run")
  @Operation(summary = "Start an AI review run")
  @RequiresProjectPermissions([Scope.BATCH_MACHINE_TRANSLATE])
  @RequiresFeatures(Feature.AI_REVIEW)
  @AllowApiAccess
  fun startAiReviewRun(
    @RequestBody @Valid request: AiReviewRunRequest,
  ): BatchJobModel {
    val job = aiReviewRunService.start(projectHolder.project.id, request, authenticationFacade.authenticatedUser.id)
    return batchJobModelAssembler.toModel(BatchJobView(job, 0, null))
  }
}
