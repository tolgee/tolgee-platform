package io.tolgee.api.v2.controllers.batch

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.BatchJobType
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
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.apiKeyAuth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.security.SecurityService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:\\d+}/start-batch-job", "/v2/projects/start-batch-job"])
@Tag(name = "Start batch jobs")
@Suppress("SpringJavaInjectionPointsAutowiringInspection", "MVCPathVariableInspection")
class StartBatchJobController(
  private val securityService: SecurityService,
  private val projectHolder: ProjectHolder,
  private val batchJobService: BatchJobService,
  private val authenticationFacade: AuthenticationFacade,
  private val batchJobModelAssembler: BatchJobModelAssembler
) {
  @PostMapping(value = ["/pre-translate-by-tm"])
  @AccessWithApiKey()
  @AccessWithProjectPermission(Scope.BATCH_PRE_TRANSLATE_BY_MT)
  @Operation(summary = "Translates provided keys to provided languages")
  fun translate(@Valid @RequestBody data: PreTranslationByTmRequest): BatchJobModel {
    securityService.checkLanguageTranslatePermission(projectHolder.project.id, data.targetLanguageIds)
    securityService.checkKeyIdsExistAndIsFromProject(data.keyIds, projectHolder.project.id)
    return batchJobService.startJob(
      data,
      projectHolder.projectEntity,
      authenticationFacade.userAccountEntity,
      BatchJobType.PRE_TRANSLATE_BY_MT
    ).model
  }

  @PostMapping(value = ["/machine-translate"])
  @AccessWithApiKey()
  @AccessWithProjectPermission(Scope.BATCH_MACHINE_TRANSLATE)
  @Operation(summary = "Translates provided keys to provided languages")
  fun machineTranslation(@Valid @RequestBody data: MachineTranslationRequest): BatchJobModel {
    securityService.checkLanguageTranslatePermission(projectHolder.project.id, data.targetLanguageIds)
    securityService.checkKeyIdsExistAndIsFromProject(data.keyIds, projectHolder.project.id)
    return batchJobService.startJob(
      data,
      projectHolder.projectEntity,
      authenticationFacade.userAccountEntity,
      BatchJobType.MACHINE_TRANSLATE
    ).model
  }

  @PostMapping(value = ["/delete-keys"])
  @AccessWithApiKey()
  @AccessWithProjectPermission(Scope.KEYS_DELETE)
  @Operation(summary = "Translates provided keys to provided languages")
  fun deleteKeys(@Valid @RequestBody data: DeleteKeysRequest): BatchJobModel {
    securityService.checkKeyIdsExistAndIsFromProject(data.keyIds, projectHolder.project.id)
    return batchJobService.startJob(
      data,
      projectHolder.projectEntity,
      authenticationFacade.userAccountEntity,
      BatchJobType.DELETE_KEYS
    ).model
  }

  @PostMapping(value = ["/set-translation-state"])
  @AccessWithApiKey()
  @AccessWithProjectPermission(Scope.TRANSLATIONS_STATE_EDIT)
  @Operation(summary = "Set translation state")
  fun setTranslationState(@Valid @RequestBody data: SetTranslationsStateStateRequest): BatchJobModel {
    securityService.checkKeyIdsExistAndIsFromProject(data.keyIds, projectHolder.project.id)
    securityService.checkLanguageStateChangePermission(projectHolder.project.id, data.languageIds)
    return batchJobService.startJob(
      data,
      projectHolder.projectEntity,
      authenticationFacade.userAccountEntity,
      BatchJobType.SET_TRANSLATIONS_STATE
    ).model
  }

  @PostMapping(value = ["/clear-translations"])
  @AccessWithApiKey()
  @AccessWithProjectPermission(Scope.TRANSLATIONS_EDIT)
  @Operation(summary = "Clear translation values")
  fun clearTranslations(@Valid @RequestBody data: ClearTranslationsRequest): BatchJobModel {
    securityService.checkKeyIdsExistAndIsFromProject(data.keyIds, projectHolder.project.id)
    securityService.checkLanguageTranslatePermission(projectHolder.project.id, data.languageIds)
    return batchJobService.startJob(
      data,
      projectHolder.projectEntity,
      authenticationFacade.userAccountEntity,
      BatchJobType.CLEAR_TRANSLATIONS
    ).model
  }

  @PostMapping(value = ["/copy-translations"])
  @AccessWithApiKey()
  @AccessWithProjectPermission(Scope.TRANSLATIONS_EDIT)
  @Operation(summary = "Clear translation values")
  fun copyTranslations(@Valid @RequestBody data: CopyTranslationRequest): BatchJobModel {
    securityService.checkKeyIdsExistAndIsFromProject(data.keyIds, projectHolder.project.id)
    securityService.checkLanguageTranslatePermission(projectHolder.project.id, data.targetLanguageIds)
    securityService.checkLanguageViewPermission(projectHolder.project.id, listOf(data.sourceLanguageId))
    return batchJobService.startJob(
      data,
      projectHolder.projectEntity,
      authenticationFacade.userAccountEntity,
      BatchJobType.COPY_TRANSLATIONS
    ).model
  }

  @PostMapping(value = ["/tag-keys"])
  @AccessWithApiKey()
  @AccessWithProjectPermission(Scope.KEYS_EDIT)
  @Operation(summary = "Tag keys")
  fun tagKeys(@Valid @RequestBody data: TagKeysRequest): BatchJobModel {
    data.tags.validate()
    securityService.checkKeyIdsExistAndIsFromProject(data.keyIds, projectHolder.project.id)
    return batchJobService.startJob(
      data,
      projectHolder.projectEntity,
      authenticationFacade.userAccountEntity,
      BatchJobType.TAG_KEYS
    ).model
  }

  @PostMapping(value = ["/untag-keys"])
  @AccessWithApiKey()
  @AccessWithProjectPermission(Scope.KEYS_EDIT)
  @Operation(summary = "Tag keys")
  fun untagKeys(@Valid @RequestBody data: UntagKeysRequest): BatchJobModel {
    securityService.checkKeyIdsExistAndIsFromProject(data.keyIds, projectHolder.project.id)
    return batchJobService.startJob(
      data,
      projectHolder.projectEntity,
      authenticationFacade.userAccountEntity,
      BatchJobType.UNTAG_KEYS
    ).model
  }

  @PostMapping(value = ["/set-keys-namespace"])
  @AccessWithApiKey()
  @AccessWithProjectPermission(Scope.KEYS_EDIT)
  @Operation(summary = "Tag keys")
  fun setKeysNamespace(@Valid @RequestBody data: SetKeysNamespaceRequest): BatchJobModel {
    securityService.checkKeyIdsExistAndIsFromProject(data.keyIds, projectHolder.project.id)
    return batchJobService.startJob(
      data,
      projectHolder.projectEntity,
      authenticationFacade.userAccountEntity,
      BatchJobType.SET_KEYS_NAMESPACE
    ).model
  }

  val BatchJob.model
    get() = batchJobModelAssembler.toModel(batchJobService.getView(this))

  private fun List<String>.validate() {
    if (this.any { it.isBlank() }) throw BadRequestException(Message.TAG_IS_BLANK)
    if (this.any { it.length > 100 }) throw BadRequestException(Message.TAG_TOO_LOG)
  }
}
