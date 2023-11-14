package io.tolgee.api.v2.controllers.cdn

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.component.cdn.CdnUploader
import io.tolgee.dtos.request.CdnDto
import io.tolgee.hateoas.cdn.CdnModel
import io.tolgee.hateoas.cdn.CdnModelAssembler
import io.tolgee.model.cdn.Cdn
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.cdn.CdnService
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
    "/v2/projects/{projectId}/cdns",
  ]
)
@Tag(name = "CDNs", description = "Endpoints for CDN management")
class CdnController(
  private val cdnService: CdnService,
  private val projectHolder: ProjectHolder,
  private val cdnModelAssembler: CdnModelAssembler,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val pagedCdnModelAssemblerExporter: PagedResourcesAssembler<Cdn>,
  private val cdnUploader: CdnUploader
) : IController {
  @PostMapping("")
  @Operation(description = "Create CDN")
  @RequiresProjectPermissions([Scope.CDN_MANAGE])
  @AllowApiAccess
  fun create(@Valid @RequestBody dto: CdnDto): CdnModel {
    val cdn = cdnService.create(projectHolder.project.id, dto)
    return cdnModelAssembler.toModel(cdn)
  }

  @PutMapping("/{id}")
  @Operation(description = "Updates CDN")
  @RequiresProjectPermissions([Scope.CDN_MANAGE])
  @AllowApiAccess
  fun update(@PathVariable id: Long, @Valid @RequestBody dto: CdnDto): CdnModel {
    val cdn = cdnService.update(projectId = projectHolder.project.id, id, dto)
    return cdnModelAssembler.toModel(cdn)
  }

  @RequiresProjectPermissions([Scope.CDN_MANAGE])
  @GetMapping("")
  @Operation(description = "List existing CDNs")
  @AllowApiAccess
  fun list(@ParameterObject pageable: Pageable): PagedModel<CdnModel> {
    val page = cdnService.getAllInProject(projectHolder.project.id, pageable)
    return pagedCdnModelAssemblerExporter.toModel(page, cdnModelAssembler)
  }

  @RequiresProjectPermissions([Scope.CDN_MANAGE])
  @DeleteMapping("/{id}")
  @Operation(description = "Delete CDN")
  @AllowApiAccess
  fun delete(@PathVariable id: Long) {
    cdnService.delete(projectHolder.project.id, id)
  }

  @RequiresProjectPermissions([Scope.CDN_MANAGE])
  @GetMapping("/{id}")
  @Operation(description = "Get CDN")
  @AllowApiAccess
  fun get(@PathVariable id: Long): CdnModel {
    return cdnModelAssembler.toModel(cdnService.get(projectHolder.project.id, id))
  }

  @RequiresProjectPermissions([Scope.CDN_PUBLISH])
  @PostMapping("/{id}")
  @Operation(description = "Publish to CDN")
  @AllowApiAccess
  fun post(@PathVariable id: Long) {
    val exporter = cdnService.get(projectHolder.project.id, id)
    cdnUploader.upload(exporter.id)
  }
}
