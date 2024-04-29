/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.dataImport

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.dtos.dataImport.SetFileNamespaceRequest
import io.tolgee.hateoas.dataImport.ImportFileIssueModel
import io.tolgee.hateoas.dataImport.ImportFileIssueModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.model.views.ImportFileIssueView
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.dataImport.ImportService
import io.tolgee.util.Logging
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:\\d+}/import", "/v2/projects/import"])
@ImportDocsTag
class V2ImportFilesController(
  private val importService: ImportService,
  private val authenticationFacade: AuthenticationFacade,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val pagedImportFileIssueResourcesAssembler: PagedResourcesAssembler<ImportFileIssueView>,
  private val projectHolder: ProjectHolder,
  private val importFileIssueModelAssembler: ImportFileIssueModelAssembler,
) : Logging {
  @PutMapping("/result/files/{fileId}/select-namespace")
  @Operation(
    description = "Sets namespace for file to import.",
    summary = "Select namespace",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun selectNamespace(
    @PathVariable fileId: Long,
    @RequestBody req: SetFileNamespaceRequest,
  ) {
    this.importService.selectNamespace(
      projectHolder.project.id,
      authenticationFacade.authenticatedUser.id,
      fileId,
      req.namespace,
    )
  }

  @GetMapping("/result/files/{importFileId}/issues")
  @Operation(
    description = "Returns issues for uploaded file.",
    summary = "Get file issues",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun getImportFileIssues(
    @PathVariable("importFileId") importFileId: Long,
    @ParameterObject pageable: Pageable,
  ): PagedModel<ImportFileIssueModel> {
    val page =
      importService.getFileIssues(
        projectHolder.project.id,
        authenticationFacade.authenticatedUser.id,
        importFileId,
        pageable,
      )
    return pagedImportFileIssueResourcesAssembler.toModel(page, importFileIssueModelAssembler)
  }
}
