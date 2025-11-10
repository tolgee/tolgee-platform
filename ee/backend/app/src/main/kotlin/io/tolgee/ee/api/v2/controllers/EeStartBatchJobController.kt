package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.LabelTranslationsRequest
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.hateoas.batch.BatchJobModel
import io.tolgee.hateoas.batch.BatchJobModelAssembler
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.security.SecurityService
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

  val BatchJob.model
    get() = batchJobModelAssembler.toModel(batchJobService.getView(this))
}
