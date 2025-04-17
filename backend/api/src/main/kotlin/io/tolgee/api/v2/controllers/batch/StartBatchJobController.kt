package io.tolgee.api.v2.controllers.batch

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.ClearTranslationsRequest
import io.tolgee.batch.request.CopyTranslationRequest
import io.tolgee.batch.request.DeleteKeysRequest
import io.tolgee.batch.request.MachineTranslationRequest
import io.tolgee.batch.request.PreTranslationByTmRequest
import io.tolgee.batch.request.SetKeysNamespaceRequest
import io.tolgee.batch.request.SetTranslationsStateStateRequest
import io.tolgee.batch.request.TagKeysRequest
import io.tolgee.batch.request.UntagKeysRequest
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
import io.tolgee.service.AiPlaygroundResultService
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
class StartBatchJobController(
  private val securityService: SecurityService,
  private val projectHolder: ProjectHolder,
  private val batchJobService: BatchJobService,
  private val authenticationFacade: AuthenticationFacade,
  private val batchJobModelAssembler: BatchJobModelAssembler,
  private val aiPlaygroundResultService: AiPlaygroundResultService,
) {
  @PostMapping(value = ["/pre-translate-by-tm"])
  @Operation(
    summary = "Pre-translate by TM",
    description = "Pre-translate provided keys to provided languages by TM.",
  )
  @RequiresProjectPermissions([Scope.BATCH_PRE_TRANSLATE_BY_TM])
  @AllowApiAccess
  fun translate(
    @Valid @RequestBody
    data: PreTranslationByTmRequest,
  ): BatchJobModel {
    securityService.checkLanguageTranslatePermission(projectHolder.project.id, data.targetLanguageIds)
    securityService.checkKeyIdsExistAndIsFromProject(data.keyIds, projectHolder.project.id)
    return batchJobService.startJob(
      data,
      projectHolder.projectEntity,
      authenticationFacade.authenticatedUserEntity,
      BatchJobType.PRE_TRANSLATE_BT_TM,
    ).model
  }

  @PostMapping(value = ["/machine-translate"])
  @Operation(
    summary = "Machine Translation",
    description = "Translate provided keys to provided languages through primary MT provider.",
  )
  @RequiresProjectPermissions([Scope.BATCH_MACHINE_TRANSLATE])
  @AllowApiAccess
  fun machineTranslation(
    @Valid @RequestBody
    data: MachineTranslationRequest,
  ): BatchJobModel {
    securityService.checkLanguageTranslatePermission(projectHolder.project.id, data.targetLanguageIds)
    securityService.checkKeyIdsExistAndIsFromProject(data.keyIds, projectHolder.project.id)
    return batchJobService.startJob(
      data,
      projectHolder.projectEntity,
      authenticationFacade.authenticatedUserEntity,
      BatchJobType.MACHINE_TRANSLATE,
    ).model
  }

  @PostMapping(value = ["/ai-playground-translate"])
  @Operation(
    summary = "Translates via llm and stores result in AiPlaygroundResult",
  )
  @RequiresProjectPermissions([Scope.PROMPTS_EDIT])
  @AllowApiAccess
  fun aiPlaygroundTranslate(
    @Valid @RequestBody
    data: MachineTranslationRequest,
  ): BatchJobModel {
    securityService.checkKeyIdsExistAndIsFromProject(data.keyIds, projectHolder.project.id)
    aiPlaygroundResultService.removeResults(projectHolder.project.id, authenticationFacade.authenticatedUserEntity.id)
    return batchJobService.startJob(
      data,
      projectHolder.projectEntity,
      authenticationFacade.authenticatedUserEntity,
      BatchJobType.AI_PLAYGROUND_TRANSLATE,
    ).model
  }

  @PostMapping(value = ["/delete-keys"])
  @Operation(summary = "Delete keys")
  @RequiresProjectPermissions([Scope.KEYS_DELETE])
  @AllowApiAccess
  fun deleteKeys(
    @Valid @RequestBody
    data: DeleteKeysRequest,
  ): BatchJobModel {
    securityService.checkKeyIdsExistAndIsFromProject(data.keyIds, projectHolder.project.id)
    return batchJobService.startJob(
      data,
      projectHolder.projectEntity,
      authenticationFacade.authenticatedUserEntity,
      BatchJobType.DELETE_KEYS,
    ).model
  }

  @PostMapping(value = ["/set-translation-state"])
  @Operation(summary = "Set translation state")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_STATE_EDIT])
  @AllowApiAccess
  fun setTranslationState(
    @Valid @RequestBody
    data: SetTranslationsStateStateRequest,
  ): BatchJobModel {
    securityService.checkKeyIdsExistAndIsFromProject(data.keyIds, projectHolder.project.id)
    securityService.checkLanguageStateChangePermission(projectHolder.project.id, data.languageIds)
    return batchJobService.startJob(
      data,
      projectHolder.projectEntity,
      authenticationFacade.authenticatedUserEntity,
      BatchJobType.SET_TRANSLATIONS_STATE,
    ).model
  }

  @PostMapping(value = ["/clear-translations"])
  @Operation(
    summary = "Clear translation values",
    description = "Clear translation values for provided keys in selected languages.",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_EDIT])
  @AllowApiAccess
  fun clearTranslations(
    @Valid @RequestBody
    data: ClearTranslationsRequest,
  ): BatchJobModel {
    securityService.checkKeyIdsExistAndIsFromProject(data.keyIds, projectHolder.project.id)
    securityService.checkLanguageTranslatePermission(projectHolder.project.id, data.languageIds)
    return batchJobService.startJob(
      data,
      projectHolder.projectEntity,
      authenticationFacade.authenticatedUserEntity,
      BatchJobType.CLEAR_TRANSLATIONS,
    ).model
  }

  @PostMapping(value = ["/copy-translations"])
  @Operation(
    summary = "Copy translation values",
    description = "Copy translation values from one language to other languages.",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_EDIT])
  @AllowApiAccess
  fun copyTranslations(
    @Valid @RequestBody
    data: CopyTranslationRequest,
  ): BatchJobModel {
    securityService.checkKeyIdsExistAndIsFromProject(data.keyIds, projectHolder.project.id)
    securityService.checkLanguageTranslatePermission(projectHolder.project.id, data.targetLanguageIds)
    securityService.checkLanguageViewPermission(projectHolder.project.id, listOf(data.sourceLanguageId))
    return batchJobService.startJob(
      data,
      projectHolder.projectEntity,
      authenticationFacade.authenticatedUserEntity,
      BatchJobType.COPY_TRANSLATIONS,
    ).model
  }

  @PostMapping(value = ["/tag-keys"])
  @Operation(summary = "Add tags")
  @RequiresProjectPermissions([Scope.KEYS_EDIT])
  @AllowApiAccess
  fun tagKeys(
    @Valid @RequestBody
    data: TagKeysRequest,
  ): BatchJobModel {
    data.tags.validate()
    securityService.checkKeyIdsExistAndIsFromProject(data.keyIds, projectHolder.project.id)
    return batchJobService.startJob(
      data,
      projectHolder.projectEntity,
      authenticationFacade.authenticatedUserEntity,
      BatchJobType.TAG_KEYS,
    ).model
  }

  @PostMapping(value = ["/untag-keys"])
  @Operation(summary = "Remove tags")
  @RequiresProjectPermissions([Scope.KEYS_EDIT])
  @AllowApiAccess
  fun untagKeys(
    @Valid @RequestBody
    data: UntagKeysRequest,
  ): BatchJobModel {
    securityService.checkKeyIdsExistAndIsFromProject(data.keyIds, projectHolder.project.id)
    return batchJobService.startJob(
      data,
      projectHolder.projectEntity,
      authenticationFacade.authenticatedUserEntity,
      BatchJobType.UNTAG_KEYS,
    ).model
  }

  @PostMapping(value = ["/set-keys-namespace"])
  @Operation(summary = "Set keys namespace")
  @RequiresProjectPermissions([Scope.KEYS_EDIT])
  @AllowApiAccess
  fun setKeysNamespace(
    @Valid @RequestBody
    data: SetKeysNamespaceRequest,
  ): BatchJobModel {
    securityService.checkKeyIdsExistAndIsFromProject(data.keyIds, projectHolder.project.id)
    return batchJobService.startJob(
      data,
      projectHolder.projectEntity,
      authenticationFacade.authenticatedUserEntity,
      BatchJobType.SET_KEYS_NAMESPACE,
    ).model
  }

  val BatchJob.model
    get() = batchJobModelAssembler.toModel(batchJobService.getView(this))

  private fun List<String>.validate() {
    if (this.any { it.isBlank() }) throw BadRequestException(Message.TAG_IS_BLANK)
    if (this.any { it.length > 100 }) throw BadRequestException(Message.TAG_TOO_LOG)
  }
}
