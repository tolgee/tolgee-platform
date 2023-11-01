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
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
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
@Tag(name = "Cdn Exporters", description = "Endpoints for CDN Exporter management")
class CdnExporterController(
  private val cdnExporterService: CdnExporterService,
  private val projectHolder: ProjectHolder,
  private val cdnExporterModelAssembler: CdnExporterModelAssembler,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val pagedCdnModelAssemblerExporter: PagedResourcesAssembler<CdnExporter>,
  private val cdnUploader: CdnUploader
) : IController {
  @PostMapping("")
  @Operation(description = "Create CDN Exporter")
  @RequiresProjectPermissions([Scope.CDN_MANAGE])
  @AllowApiAccess
  fun create(@Valid @RequestBody dto: CdnExporterDto): CdnExporterModel {
    val cdn = cdnExporterService.create(projectHolder.project.id, dto)
    return cdnExporterModelAssembler.toModel(cdn)
  }

  @PutMapping("/{id}")
  @Operation(description = "Updates CDN Exporter")
  @RequiresProjectPermissions([Scope.CDN_MANAGE])
  @AllowApiAccess
  fun update(@PathVariable id: Long, @Valid @RequestBody dto: CdnExporterDto): CdnExporterModel {
    val cdn = cdnExporterService.update(projectId = projectHolder.project.id, id, dto)
    return cdnExporterModelAssembler.toModel(cdn)
  }

  @RequiresProjectPermissions([Scope.CDN_MANAGE])
  @GetMapping("")
  @Operation(description = "List existing CDNs Exporters")
  @AllowApiAccess
  fun list(@ParameterObject pageable: Pageable): PagedModel<CdnExporterModel> {
    val page = cdnExporterService.getAllInProject(projectHolder.project.id, pageable)
    return pagedCdnModelAssemblerExporter.toModel(page, cdnExporterModelAssembler)
  }

  @RequiresProjectPermissions([Scope.CDN_MANAGE])
  @DeleteMapping("/{id}")
  @Operation(description = "Delete CDN Exporter")
  @AllowApiAccess
  fun delete(@PathVariable id: Long) {
    cdnExporterService.delete(id)
  }

  @RequiresProjectPermissions([Scope.CDN_MANAGE])
  @GetMapping("/{id}")
  @Operation(description = "Get CDN Exporter")
  @AllowApiAccess
  fun get(@PathVariable id: Long): CdnExporterModel {
    return cdnExporterModelAssembler.toModel(cdnExporterService.get(id))
  }

  @RequiresProjectPermissions([Scope.CDN_PUBLISH])
  @PostMapping("/{id}")
  @Operation(description = "Publish to CDN Exporter")
  @AllowApiAccess
  fun post(@PathVariable id: Long) {
    val exporter = cdnExporterService.get(id)
    cdnUploader.upload(exporter.id)
  }
}
