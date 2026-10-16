package io.tolgee.ee.api.v2.controllers.aiReview

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.batch.BatchJobService
import io.tolgee.constants.Feature
import io.tolgee.constants.FeatureFlag
import io.tolgee.ee.api.v2.hateoas.model.aiReview.AiReviewRunPreviewModel
import io.tolgee.ee.data.aiReview.AiReviewRunRequest
import io.tolgee.ee.service.aiReview.AiReviewRunService
import io.tolgee.hateoas.batch.BatchJobModel
import io.tolgee.hateoas.batch.BatchJobModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiUnstableOperationExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.ReadOnlyOperation
import io.tolgee.security.authorization.RequiresFeatureFlag
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.security.SecurityService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:[0-9]+}/ai-review/run", "/v2/projects/ai-review/run"])
@Tag(name = "AI review")
@OpenApiUnstableOperationExtension
@RequiresFeatureFlag(FeatureFlag.AI_REVIEW)
class AiReviewRunController(
  private val projectHolder: ProjectHolder,
  private val aiReviewRunService: AiReviewRunService,
  private val securityService: SecurityService,
  private val authenticationFacade: AuthenticationFacade,
  private val batchJobService: BatchJobService,
  private val batchJobModelAssembler: BatchJobModelAssembler,
) {
  @PostMapping("/preview")
  @Operation(
    summary = "Preview an AI review run",
    operationId = "previewAiReviewRun",
    description =
      "Counts which of the selected translations an AI review run would review and why the others are skipped. " +
        "Makes no LLM calls.",
  )
  @ReadOnlyOperation
  @RequiresProjectPermissions([Scope.TRANSLATIONS_STATE_EDIT, Scope.BATCH_MACHINE_TRANSLATE])
  @AllowApiAccess
  @RequiresFeatures(Feature.AI_REVIEW)
  fun preview(
    @RequestBody request: AiReviewRunRequest,
  ): AiReviewRunPreviewModel {
    checkPermissions(request)
    val preview = aiReviewRunService.preview(projectHolder.project, request)
    return AiReviewRunPreviewModel(
      total = preview.total,
      withoutRereview = preview.withoutRereview,
      withRereview = preview.withRereview,
    )
  }

  @PostMapping("")
  @Operation(
    summary = "Start an AI review run",
    operationId = "startAiReviewRun",
    description =
      "Starts an AI_REVIEW batch job that reviews the selected keys, or all translations a rule covers, " +
        "using the same rules, order and checks as automatic AI review. Returns the batch job; track and " +
        "cancel it with the batch job endpoints like any other batch operation.",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_STATE_EDIT, Scope.BATCH_MACHINE_TRANSLATE])
  @AllowApiAccess
  @RequiresFeatures(Feature.AI_REVIEW)
  fun start(
    @RequestBody request: AiReviewRunRequest,
  ): BatchJobModel {
    checkPermissions(request)
    val job =
      aiReviewRunService.start(projectHolder.project, request, authenticationFacade.authenticatedUserEntity)
    return batchJobModelAssembler.toModel(batchJobService.getView(job))
  }

  private fun checkPermissions(request: AiReviewRunRequest) {
    val projectId = projectHolder.project.id
    request.keyIds?.let { securityService.checkKeyIdsExistAndIsFromProject(it, projectId) }
    val languageIds = aiReviewRunService.getCandidateLanguageIds(projectHolder.project, request)
    securityService.checkLanguageStateChangePermission(projectId, languageIds)
  }
}
