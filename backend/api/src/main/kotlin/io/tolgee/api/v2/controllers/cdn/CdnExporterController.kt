package io.tolgee.api.v2.controllers.cdn

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.component.cdn.CdnUploader
import io.tolgee.dtos.request.CdnExporterDto
import io.tolgee.hateoas.cdn.CdnExporterModel
import io.tolgee.hateoas.cdn.CdnExporterModelAssembler
import io.tolgee.model.cdn.CdnExporter
import io.tolgee.model.enums.Scope
import io.tolgee.security.apiKeyAuth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.cdn.CdnExporterService
import org.springdoc.api.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId}/cdn-exporters",
  ]
)
@Tag(name = "Cdn management", description = "Endpoints for CDN management")
class CdnExporterController(
  private val cdnExporterService: CdnExporterService,
  private val projectHolder: ProjectHolder,
  private val cdnExporterModelAssembler: CdnExporterModelAssembler,
  private val pagedCdnModelAssemblerExporter: PagedResourcesAssembler<CdnExporter>,
  private val cdnUploader: CdnUploader
) : IController {
  @PostMapping("")
  @Operation(description = "Create CDN Exporter")
  @AccessWithProjectPermission(scope = Scope.CDN_MANAGE)
  @AccessWithApiKey
  fun createCdnExporter(@Valid @RequestBody dto: CdnExporterDto): CdnExporterModel {
    val cdn = cdnExporterService.create(projectHolder.project.id, dto)
    return cdnExporterModelAssembler.toModel(cdn)
  }

  @PutMapping("/{cdnId}")
  @Operation(description = "Updates CDN Exporter")
  @AccessWithProjectPermission(scope = Scope.CDN_MANAGE)
  @AccessWithApiKey
  fun update(@PathVariable cdnId: Long, @Valid @RequestBody dto: CdnExporterDto): CdnExporterModel {
    val cdn = cdnExporterService.update(cdnId, dto)
    return cdnExporterModelAssembler.toModel(cdn)
  }

  @AccessWithProjectPermission(scope = Scope.CDN_MANAGE)
  @GetMapping("")
  @Operation(description = "List existing CDNs Exporters")
  @AccessWithApiKey
  fun list(@ParameterObject pageable: Pageable): PagedModel<CdnExporterModel> {
    val page = cdnExporterService.getAllInProject(projectHolder.project.id, pageable)
    return pagedCdnModelAssemblerExporter.toModel(page, cdnExporterModelAssembler)
  }

  @AccessWithProjectPermission(scope = Scope.CDN_MANAGE)
  @DeleteMapping("/{cdnId}")
  @Operation(description = "Delete CDN Exporter")
  @AccessWithApiKey
  fun delete(@PathVariable cdnId: Long) {
    cdnExporterService.delete(cdnId)
  }

  @AccessWithProjectPermission(scope = Scope.CDN_MANAGE)
  @GetMapping("/{cdnId}")
  @Operation(description = "Get CDN Exporter")
  @AccessWithApiKey
  fun get(@PathVariable cdnId: Long): CdnExporterModel {
    return cdnExporterModelAssembler.toModel(cdnExporterService.get(cdnId))
  }

  @AccessWithProjectPermission(scope = Scope.CDN_PUBLISH)
  @PostMapping("/{cdnId}")
  @Operation(description = "Publish to CDN Exporter")
  @AccessWithApiKey
  fun post(@PathVariable cdnId: Long) {
    val cdn = cdnExporterService.get(cdnId)
    cdnUploader.upload(cdn.id)
  }
}
