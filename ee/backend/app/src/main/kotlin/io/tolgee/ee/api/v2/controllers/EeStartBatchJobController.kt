package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.request.LabelTranslationsRequest
import io.tolgee.batch.request.QaCheckRequest
import io.tolgee.batch.request.QaRecheckByKeysRequest
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.hateoas.batch.BatchJobModel
import io.tolgee.hateoas.batch.BatchJobModelAssembler
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:\\d+}/start-batch-job", "/v2/projects/start-batch-job"])
@Tag(name = "Batch Operations")
@Suppress("MVCPathVariableInspection")
class EeStartBatchJobController(
  private val securityService: SecurityService,
  private val projectHolder: ProjectHolder,
  private val batchJobService: BatchJobService,
  private val authenticationFacade: AuthenticationFacade,
  private val batchJobModelAssembler: BatchJobModelAssembler,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
  private val translationService: TranslationService,
  private val languageService: LanguageService,
  private val projectFeatureGuard: ProjectFeatureGuard,
) {
  @PostMapping(value = ["/assign-translation-label"])
  @Operation(
    summary = "Assign labels to translations",
  )
  @RequiresProjectPermissions([Scope.TRANSLATION_LABEL_ASSIGN])
  @AllowApiAccess
  fun assignTranslationLabel(
    @Valid @RequestBody
    data: LabelTranslationsRequest,
  ): BatchJobModel {
    enabledFeaturesProvider.checkFeatureEnabled(
      projectHolder.project.organizationOwnerId,
      Feature.TRANSLATION_LABELS,
    )
    securityService.checkKeyIdsExistAndIsFromProject(data.keyIds, projectHolder.project.id)
    securityService.checkLabelIdsExistAndIsFromProject(data.labelIds, projectHolder.project.id)
    return batchJobService
      .startJob(
        data,
        projectHolder.projectEntity,
        authenticationFacade.authenticatedUserEntity,
        BatchJobType.ASSIGN_TRANSLATION_LABEL,
      ).model
  }

  @PostMapping(value = ["/unassign-translation-label"])
  @Operation(
    summary = "Unassign labels from translations",
  )
  @RequiresProjectPermissions([Scope.TRANSLATION_LABEL_ASSIGN])
  @AllowApiAccess
  fun unassignTranslationLabel(
    @Valid @RequestBody
    data: LabelTranslationsRequest,
  ): BatchJobModel {
    enabledFeaturesProvider.checkFeatureEnabled(
      projectHolder.project.organizationOwnerId,
      Feature.TRANSLATION_LABELS,
    )
    securityService.checkKeyIdsExistAndIsFromProject(data.keyIds, projectHolder.project.id)
    securityService.checkLabelIdsExistAndIsFromProject(data.labelIds, projectHolder.project.id)
    return batchJobService
      .startJob(
        data,
        projectHolder.projectEntity,
        authenticationFacade.authenticatedUserEntity,
        BatchJobType.UNASSIGN_TRANSLATION_LABEL,
      ).model
  }

  @PostMapping(value = ["/qa-check"])
  @Operation(
    summary = "Rerun QA checks for translations of selected keys",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_EDIT])
  @AllowApiAccess
  fun qaCheck(
    @Valid @RequestBody
    data: QaRecheckByKeysRequest,
  ): BatchJobModel {
    projectFeatureGuard.checkEnabled(Feature.QA_CHECKS)
    val projectId = projectHolder.project.id
    securityService.checkKeyIdsExistAndIsFromProject(data.keyIds, projectId)
    if (data.languageIds != null) {
      securityService.checkLanguageTranslatePermission(projectId, data.languageIds!!)
    }

    val languageIds =
      data.languageIds
        ?: languageService.getProjectLanguages(projectId).map { it.id }

    val target =
      data.keyIds.flatMap { keyId ->
        languageIds.map { langId -> BatchTranslationTargetItem(keyId = keyId, languageId = langId) }
      }
    if (target.isEmpty()) {
      throw BadRequestException(Message.NO_TRANSLATIONS_TO_RECHECK)
    }

    // Mark all existing translations targeted by the job as stale
    val existingTranslationIds = translationService.getTranslationIdsByKeyIds(data.keyIds, languageIds)
    if (existingTranslationIds.isNotEmpty()) {
      translationService.setQaChecksStale(existingTranslationIds)
    }

    return batchJobService
      .startJob(
        QaCheckRequest(target = target),
        projectHolder.projectEntity,
        authenticationFacade.authenticatedUserEntity,
        BatchJobType.QA_CHECK,
      ).model
  }

  val BatchJob.model
    get() = batchJobModelAssembler.toModel(batchJobService.getView(this))
}
