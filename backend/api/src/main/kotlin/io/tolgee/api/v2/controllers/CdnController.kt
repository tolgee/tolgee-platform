package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.cdn.CdnUploader
import io.tolgee.dtos.request.CdnDto
import io.tolgee.hateoas.cdn.CdnModel
import io.tolgee.hateoas.cdn.CdnModelAssembler
import io.tolgee.model.cdn.Cdn
import io.tolgee.model.enums.Scope
import io.tolgee.security.apiKeyAuth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.CdnService
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
@Tag(name = "Cdn management", description = "Endpoints for CDN management")
class CdnController(
  private val cdnService: CdnService,
  private val projectHolder: ProjectHolder,
  private val cdnModelAssembler: CdnModelAssembler,
  private val pagedCdnModelAssembler: PagedResourcesAssembler<Cdn>,
  private val cdnUploader: CdnUploader
) : IController {
  @PostMapping("")
  @Operation(description = "Create CDN")
  @AccessWithProjectPermission(scope = Scope.CDN_MANAGE)
  @AccessWithApiKey
  fun createCdn(@Valid @RequestBody dto: CdnDto): CdnModel {
    val cdn = cdnService.create(projectHolder.project.id, dto)
    return cdnModelAssembler.toModel(cdn)
  }

  @PutMapping("/{cdnId}")
  @Operation(description = "Updates CDN")
  @AccessWithProjectPermission(scope = Scope.CDN_MANAGE)
  @AccessWithApiKey
  fun update(@PathVariable cdnId: Long, @Valid @RequestBody dto: CdnDto): CdnModel {
    val cdn = cdnService.update(cdnId, dto)
    return cdnModelAssembler.toModel(cdn)
  }

  @AccessWithProjectPermission(scope = Scope.CDN_MANAGE)
  @GetMapping("")
  @Operation(description = "List existing CDNs")
  @AccessWithApiKey
  fun list(@ParameterObject pageable: Pageable): PagedModel<CdnModel> {
    val page = cdnService.getAllInProject(projectHolder.project.id, pageable)
    return pagedCdnModelAssembler.toModel(page, cdnModelAssembler)
  }

  @AccessWithProjectPermission(scope = Scope.CDN_MANAGE)
  @DeleteMapping("/{cdnId}")
  @Operation(description = "Delete CDN")
  @AccessWithApiKey
  fun delete(@PathVariable cdnId: Long) {
    cdnService.delete(cdnId)
  }

  @AccessWithProjectPermission(scope = Scope.CDN_MANAGE)
  @GetMapping("/{cdnId}")
  @Operation(description = "Get CDN")
  @AccessWithApiKey
  fun get(@PathVariable cdnId: Long): CdnModel {
    return cdnModelAssembler.toModel(cdnService.get(cdnId))
  }

  @AccessWithProjectPermission(scope = Scope.CDN_PUBLISH)
  @PostMapping("/{cdnId}")
  @Operation(description = "Publish to CDN")
  @AccessWithApiKey
  fun post(@PathVariable cdnId: Long) {
    val cdn = cdnService.get(cdnId)
    cdnUploader.upload(cdn.id, cdn.exportParams)
  }
}
