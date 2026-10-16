package io.tolgee.ee.api.v2.controllers.aiReview

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Feature
import io.tolgee.constants.FeatureFlag
import io.tolgee.ee.api.v2.hateoas.assemblers.aiReview.TranslationAiReviewModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.aiReview.TranslationAiReviewModel
import io.tolgee.ee.service.aiReview.TranslationAiReviewService
import io.tolgee.hateoas.translations.TranslationModel
import io.tolgee.hateoas.translations.TranslationModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiUnstableOperationExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.ReadOnlyOperation
import io.tolgee.security.authorization.RequiresFeatureFlag
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.security.authorization.UseDefaultPermissions
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
@RequestMapping(
  value = ["/v2/projects/{projectId:[0-9]+}/ai-review/translations", "/v2/projects/ai-review/translations"],
)
@Tag(name = "AI review")
@OpenApiUnstableOperationExtension
@RequiresFeatureFlag(FeatureFlag.AI_REVIEW)
class AiReviewTranslationController(
  private val projectHolder: ProjectHolder,
  private val translationAiReviewService: TranslationAiReviewService,
  private val translationAiReviewModelAssembler: TranslationAiReviewModelAssembler,
  private val translationModelAssembler: TranslationModelAssembler,
  private val securityService: SecurityService,
  private val authenticationFacade: AuthenticationFacade,
  private val translationService: TranslationService,
) {
  @GetMapping("/{translationId:[0-9]+}")
  @Operation(summary = "Get AI review result of a translation", operationId = "getTranslationAiReview")
  @ReadOnlyOperation
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  @RequiresFeatures(Feature.AI_REVIEW)
  fun get(
    @PathVariable translationId: Long,
  ): TranslationAiReviewModel {
    val projectId = projectHolder.project.id
    val translation = translationService.get(projectId, translationId)
    securityService.checkLanguageViewPermission(projectId, listOf(translation.language.id))
    return translationAiReviewModelAssembler.toModel(translationAiReviewService.getDetail(translation))
  }

  @PostMapping("/{translationId:[0-9]+}/confirm")
  @Operation(
    summary = "Confirm AI review",
    operationId = "confirmTranslationAiReview",
    description = "Keeps the translation reviewed and turns the AI review into a regular review.",
  )
  @UseDefaultPermissions
  @AllowApiAccess
  @RequiresFeatures(Feature.AI_REVIEW)
  fun confirm(
    @PathVariable translationId: Long,
  ): TranslationAiReviewModel {
    val review =
      translationAiReviewService.confirm(
        projectHolder.project.id,
        translationId,
        authenticationFacade.authenticatedUserEntity,
      )
    return translationAiReviewModelAssembler.toModel(review)
  }

  @PostMapping("/{translationId:[0-9]+}/retry")
  @Operation(
    summary = "Retry failed AI review",
    operationId = "retryTranslationAiReview",
    description = "Runs the AI review again for a translation whose last review failed.",
  )
  @UseDefaultPermissions
  @AllowApiAccess
  @RequiresFeatures(Feature.AI_REVIEW)
  fun retry(
    @PathVariable translationId: Long,
  ) {
    translationAiReviewService.retry(projectHolder.project.id, translationId)
  }

  @PostMapping("/{translationId:[0-9]+}/reject")
  @Operation(
    summary = "Reject AI review",
    operationId = "rejectTranslationAiReview",
    description = "Sets the translation back to translated and records the rejection.",
  )
  @UseDefaultPermissions
  @AllowApiAccess
  @RequiresFeatures(Feature.AI_REVIEW)
  @RequestActivity(ActivityType.SET_TRANSLATION_STATE)
  fun reject(
    @PathVariable translationId: Long,
  ): TranslationModel {
    val translation =
      translationAiReviewService.reject(
        projectHolder.project.id,
        translationId,
        authenticationFacade.authenticatedUserEntity,
      )
    return translationModelAssembler.toModel(translation)
  }
}
