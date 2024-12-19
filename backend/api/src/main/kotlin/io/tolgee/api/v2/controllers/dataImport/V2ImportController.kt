/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.dataImport

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.dtos.dataImport.ImportAddFilesParams
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.exceptions.ErrorResponseBody
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.dataImport.ImportAddFilesResultModel
import io.tolgee.hateoas.dataImport.ImportLanguageModel
import io.tolgee.hateoas.dataImport.ImportLanguageModelAssembler
import io.tolgee.hateoas.dataImport.ImportNamespaceModel
import io.tolgee.model.enums.Scope
import io.tolgee.model.views.ImportLanguageView
import io.tolgee.openApiDocs.OpenApiHideFromPublicDocs
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.dataImport.ForceMode
import io.tolgee.service.dataImport.ImportService
import io.tolgee.service.key.NamespaceService
import io.tolgee.util.Logging
import io.tolgee.util.StreamingResponseBodyProvider
import io.tolgee.util.filterFiles
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.PagedModel
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:\\d+}/import", "/v2/projects/import"])
@ImportDocsTag
@OpenApiOrderExtension(5)
class V2ImportController(
  private val importService: ImportService,
  private val authenticationFacade: AuthenticationFacade,
  private val importLanguageModelAssembler: ImportLanguageModelAssembler,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val pagedLanguagesResourcesAssembler: PagedResourcesAssembler<ImportLanguageView>,
  private val projectHolder: ProjectHolder,
  private val namespaceService: NamespaceService,
  private val streamingResponseBodyProvider: StreamingResponseBodyProvider,
  private val streamingImportProgressUtil: StreamingImportProgressUtil,
) : Logging {
  @PostMapping("", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @Operation(description = "Prepares provided files to import.", summary = "Add files")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  @OpenApiOrderExtension(1)
  fun addFiles(
    @RequestPart("files") files: Array<MultipartFile>,
    @ParameterObject params: ImportAddFilesParams,
  ): ImportAddFilesResultModel {
    val filteredFiles = filterFiles(files.map { (it.originalFilename ?: "") to it })
    val fileDtos =
      filteredFiles.map {
        ImportFileDto(it.originalFilename ?: "", it.inputStream.readAllBytes())
      }
    val (errors, warnings) =
      importService.addFiles(
        files = fileDtos,
        project = projectHolder.projectEntity,
        userAccount = authenticationFacade.authenticatedUserEntity,
        params = params,
      )
    return getImportAddFilesResultModel(errors, warnings)
  }

  private fun getImportAddFilesResultModel(
    errors: List<ErrorResponseBody>,
    warnings: List<ErrorResponseBody>,
  ): ImportAddFilesResultModel {
    val result: PagedModel<ImportLanguageModel>? =
      try {
        this.getImportResult(PageRequest.of(0, 100))
      } catch (e: NotFoundException) {
        null
      }
    return ImportAddFilesResultModel(errors, warnings, result)
  }

  @PutMapping("/apply")
  @Operation(summary = "Apply import", description = "Imports the data prepared in previous step")
  @RequestActivity(ActivityType.IMPORT)
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  @OpenApiOrderExtension(2)
  fun applyImport(
    @Parameter(description = "Whether override or keep all translations with unresolved conflicts")
    @RequestParam(defaultValue = "NO_FORCE")
    forceMode: ForceMode,
  ) {
    val projectId = projectHolder.project.id
    this.importService.import(projectId, authenticationFacade.authenticatedUser.id, forceMode)
  }

  @PutMapping("/apply-streaming", produces = [MediaType.APPLICATION_NDJSON_VALUE])
  @Operation(
    summary = "Apply import (streaming)",
    description =
      "Imports the data prepared in previous step. Streams current status.",
  )
  @RequestActivity(ActivityType.IMPORT)
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  @OpenApiHideFromPublicDocs
  fun applyImportStreaming(
    @Parameter(description = "Whether override or keep all translations with unresolved conflicts")
    @RequestParam(defaultValue = "NO_FORCE")
    forceMode: ForceMode,
  ): ResponseEntity<StreamingResponseBody> {
    val projectId = projectHolder.project.id

    return streamingImportProgressUtil.stream { writeStatus ->
      this.importService.import(projectId, authenticationFacade.authenticatedUser.id, forceMode, writeStatus)
    }
  }

  @GetMapping("/result")
  @Operation(description = "Returns the result of preparation.", summary = "Get result")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun getImportResult(
    @ParameterObject pageable: Pageable,
  ): PagedModel<ImportLanguageModel> {
    val projectId = projectHolder.project.id
    val userId = authenticationFacade.authenticatedUser.id
    val languages = importService.getResult(projectId, userId, pageable)
    return pagedLanguagesResourcesAssembler.toModel(languages, importLanguageModelAssembler)
  }

  @DeleteMapping("")
  @Operation(description = "Deletes prepared import data.", summary = "Delete")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  @OpenApiOrderExtension(3)
  fun cancelImport() {
    this.importService.deleteImport(projectHolder.project.id, authenticationFacade.authenticatedUser.id)
  }

  @GetMapping("/all-namespaces")
  @Operation(
    description = "Returns all existing and imported namespaces",
    summary = "Get namespaces",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun getAllNamespaces(): CollectionModel<ImportNamespaceModel> {
    val import =
      importService.get(
        projectId = projectHolder.project.id,
        authorId = authenticationFacade.authenticatedUser.id,
      )
    val importNamespaces = importService.getAllNamespaces(import.id)
    val existingNamespaces = namespaceService.getAllInProject(projectId = projectHolder.project.id)
    val result =
      existingNamespaces.associateTo(mutableMapOf()) { it.name to ImportNamespaceModel(it.id, it.name) }
    importNamespaces.filterNotNull().forEach { importNamespace ->
      result.computeIfAbsent(importNamespace) {
        ImportNamespaceModel(id = null, name = importNamespace)
      }
    }

    return getNamespacesCollectionModel(result)
  }

  private fun getNamespacesCollectionModel(
    result: MutableMap<String, ImportNamespaceModel>,
  ): CollectionModel<ImportNamespaceModel> {
    val assembler =
      object : RepresentationModelAssemblerSupport<ImportNamespaceModel, ImportNamespaceModel>(
        this::class.java,
        ImportNamespaceModel::class.java,
      ) {
        override fun toModel(entity: ImportNamespaceModel): ImportNamespaceModel = entity
      }

    return assembler.toCollectionModel(result.values.sortedBy { it.name })
  }
}
