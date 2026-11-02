package io.tolgee.ee.api.v2.controllers.aiReview

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.ee.api.v2.hateoas.assemblers.aiReview.TranslationAiReviewModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.aiReview.TranslationAiReviewModel
import io.tolgee.ee.service.aiReview.AiReviewActionService
import io.tolgee.model.enums.Scope
import io.tolgee.model.translation.Translation
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/v2/projects/{projectId:[0-9]+}/translations/{translationId:[0-9]+}/ai-review")
@Tag(name = "AI review of a translation")
class TranslationAiReviewController(
  private val aiReviewActionService: AiReviewActionService,
  private val translationAiReviewModelAssembler: TranslationAiReviewModelAssembler,
  private val translationService: TranslationService,
  private val securityService: SecurityService,
  private val projectHolder: ProjectHolder,
  private val authenticationFacade: AuthenticationFacade,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
) {
  @GetMapping("")
  @Operation(summary = "Get the AI review of a translation")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  fun getAiReview(
    @PathVariable translationId: Long,
  ): TranslationAiReviewModel {
    checkAiReviewEnabled()
    val translation = loadTranslation(translationId)
    securityService.checkLanguageViewPermission(projectHolder.project.id, listOf(translation.language.id))
    return translationAiReviewModelAssembler.toModel(aiReviewActionService.get(translationId))
  }

  @PutMapping("/confirm")
  @Operation(summary = "Confirm an AI approval")
  @RequestActivity(ActivityType.SET_TRANSLATION_STATE)
  @UseDefaultPermissions
  fun confirmAiReview(
    @PathVariable translationId: Long,
  ): TranslationAiReviewModel {
    checkAiReviewEnabled()
    val translation = checkedForStateChange(translationId)
    val row = aiReviewActionService.confirm(translation, authenticationFacade.authenticatedUser.id)
    return translationAiReviewModelAssembler.toModel(row)
  }

  @PutMapping("/reject")
  @Operation(summary = "Reject an AI approval")
  @RequestActivity(ActivityType.SET_TRANSLATION_STATE)
  @UseDefaultPermissions
  fun rejectAiReview(
    @PathVariable translationId: Long,
  ): TranslationAiReviewModel {
    checkAiReviewEnabled()
    val translation = checkedForStateChange(translationId)
    val row = aiReviewActionService.reject(translation, authenticationFacade.authenticatedUser.id)
    return translationAiReviewModelAssembler.toModel(row)
  }

  private fun checkedForStateChange(translationId: Long): Translation {
    val translation = loadTranslation(translationId)
    securityService.checkStateChangePermission(translation)
    securityService.checkBranchModify(translation)
    return translation
  }

  private fun loadTranslation(translationId: Long): Translation =
    translationService.get(projectHolder.project.id, translationId)

  private fun checkAiReviewEnabled() {
    enabledFeaturesProvider.checkFeatureEnabled(projectHolder.project.organizationOwnerId, Feature.AI_REVIEW)
  }
}
