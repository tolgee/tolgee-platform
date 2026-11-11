package io.tolgee.ee.api.v2.controllers.aiReview

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Feature
import io.tolgee.ee.api.v2.hateoas.assemblers.aiReview.TranslationAiReviewModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.aiReview.TranslationAiReviewModel
import io.tolgee.ee.data.aiReview.AiReviewRunRequest
import io.tolgee.ee.service.aiReview.AiReviewActionService
import io.tolgee.ee.service.aiReview.AiReviewRunService
import io.tolgee.hateoas.batch.BatchJobModel
import io.tolgee.hateoas.batch.BatchJobModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.model.translation.Translation
import io.tolgee.model.views.BatchJobView
import io.tolgee.openApiDocs.OpenApiUnstableOperationExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/v2/projects/{projectId:[0-9]+}/translations/{translationId:[0-9]+}/ai-review")
@Tag(name = "AI review of a translation")
@OpenApiUnstableOperationExtension
class TranslationAiReviewController(
  private val aiReviewActionService: AiReviewActionService,
  private val translationAiReviewModelAssembler: TranslationAiReviewModelAssembler,
  private val translationService: TranslationService,
  private val securityService: SecurityService,
  private val projectHolder: ProjectHolder,
  private val authenticationFacade: AuthenticationFacade,
  private val aiReviewRunService: AiReviewRunService,
  private val batchJobModelAssembler: BatchJobModelAssembler,
  private val projectFeatureGuard: ProjectFeatureGuard,
) {
  @GetMapping("")
  @Operation(summary = "Get the AI review of a translation")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @RequiresFeatures(Feature.AI_REVIEW)
  @AllowApiAccess
  fun getAiReview(
    @PathVariable translationId: Long,
  ): TranslationAiReviewModel {
    projectFeatureGuard.checkEnabled(Feature.AI_REVIEW)
    val translation = loadTranslation(translationId)
    securityService.checkLanguageViewPermission(projectHolder.project.id, listOf(translation.language.id))
    return translationAiReviewModelAssembler.toModel(aiReviewActionService.get(translationId))
  }

  @PostMapping("/retry")
  @Operation(summary = "Run the AI review again for one translation")
  @RequiresProjectPermissions([Scope.BATCH_MACHINE_TRANSLATE])
  @RequiresFeatures(Feature.AI_REVIEW)
  @AllowApiAccess
  fun retryAiReview(
    @PathVariable translationId: Long,
  ): BatchJobModel {
    projectFeatureGuard.checkEnabled(Feature.AI_REVIEW)
    val translation = loadTranslation(translationId)
    securityService.checkBranchModify(translation)
    aiReviewActionService.checkRetryAllowed(translationId)
    val job =
      aiReviewRunService.start(
        projectHolder.project.id,
        AiReviewRunRequest(translationIds = listOf(translationId), reReview = true),
        authenticationFacade.authenticatedUser.id,
      )
    return batchJobModelAssembler.toModel(BatchJobView(job, 0, null))
  }

  private fun loadTranslation(translationId: Long): Translation =
    translationService.get(projectHolder.project.id, translationId)
}
