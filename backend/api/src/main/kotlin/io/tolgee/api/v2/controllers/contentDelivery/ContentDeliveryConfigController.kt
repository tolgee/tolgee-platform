package io.tolgee.api.v2.controllers.contentDelivery

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.component.contentDelivery.ContentDeliveryUploader
import io.tolgee.dtos.request.ContentDeliveryConfigRequest
import io.tolgee.hateoas.contentDelivery.ContentDeliveryConfigModel
import io.tolgee.hateoas.contentDelivery.ContentDeliveryConfigModelAssembler
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.contentDelivery.ContentDeliveryConfigService
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
    "/v2/projects/{projectId}/content-delivery-configs",
  ]
)
@Tag(name = "Content Delivery", description = "Endpoints for Content Delivery management")
class ContentDeliveryConfigController(
  private val contentDeliveryService: ContentDeliveryConfigService,
  private val projectHolder: ProjectHolder,
  private val contentDeliveryConfigModelAssembler: ContentDeliveryConfigModelAssembler,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val pagedContentDeliveryConfigModelAssemblerExporter: PagedResourcesAssembler<ContentDeliveryConfig>,
  private val contentDeliveryUploader: ContentDeliveryUploader
) : IController {
  @PostMapping("")
  @Operation(description = "Create Content Delivery Config")
  @RequiresProjectPermissions([Scope.CONTENT_DELIVERY_MANAGE])
  @AllowApiAccess
  fun create(@Valid @RequestBody dto: ContentDeliveryConfigRequest): ContentDeliveryConfigModel {
    val contentDeliveryConfig = contentDeliveryService.create(projectHolder.project.id, dto)
    return contentDeliveryConfigModelAssembler.toModel(contentDeliveryConfig)
  }

  @PutMapping("/{id}")
  @Operation(description = "Updates Content Delivery Config")
  @RequiresProjectPermissions([Scope.CONTENT_DELIVERY_MANAGE])
  @AllowApiAccess
  fun update(
    @PathVariable id: Long,
    @Valid @RequestBody dto: ContentDeliveryConfigRequest
  ): ContentDeliveryConfigModel {
    val contentDeliveryConfig = contentDeliveryService.update(projectId = projectHolder.project.id, id, dto)
    return contentDeliveryConfigModelAssembler.toModel(contentDeliveryConfig)
  }

  @RequiresProjectPermissions([Scope.CONTENT_DELIVERY_PUBLISH])
  @GetMapping("")
  @Operation(description = "List existing Content Delivery Configs")
  @AllowApiAccess
  fun list(@ParameterObject pageable: Pageable): PagedModel<ContentDeliveryConfigModel> {
    val page = contentDeliveryService.getAllInProject(projectHolder.project.id, pageable)
    return pagedContentDeliveryConfigModelAssemblerExporter.toModel(page, contentDeliveryConfigModelAssembler)
  }

  @RequiresProjectPermissions([Scope.CONTENT_DELIVERY_MANAGE])
  @DeleteMapping("/{id}")
  @Operation(description = "Delete Content Delivery Config")
  @AllowApiAccess
  fun delete(@PathVariable id: Long) {
    contentDeliveryService.delete(projectHolder.project.id, id)
  }

  @RequiresProjectPermissions([Scope.CONTENT_DELIVERY_PUBLISH])
  @GetMapping("/{id}")
  @Operation(description = "Get Content Delivery Config")
  @AllowApiAccess
  fun get(@PathVariable id: Long): ContentDeliveryConfigModel {
    return contentDeliveryConfigModelAssembler.toModel(contentDeliveryService.get(projectHolder.project.id, id))
  }

  @RequiresProjectPermissions([Scope.CONTENT_DELIVERY_PUBLISH])
  @PostMapping("/{id}")
  @Operation(description = "Publish to Content Delivery")
  @AllowApiAccess
  fun post(@PathVariable id: Long) {
    val exporter = contentDeliveryService.get(projectHolder.project.id, id)
    contentDeliveryUploader.upload(exporter.id)
  }
}
