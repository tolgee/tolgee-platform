package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.dtos.contentDelivery.ContentStorageRequest
import io.tolgee.ee.api.v2.hateoas.contentStorage.ContentStorageModel
import io.tolgee.ee.api.v2.hateoas.contentStorage.ContentStorageModelAssembler
import io.tolgee.ee.data.StorageTestResult
import io.tolgee.ee.service.ContentStorageService
import io.tolgee.model.contentDelivery.ContentStorage
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
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

@Suppress("MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId}/content-storages",
  ]
)
@Tag(name = "Content Storages management (EE)")
class ContentStorageController(
  private val contentStorageService: ContentStorageService,
  private val projectHolder: ProjectHolder,
  private val contentStorageModelAssembler: ContentStorageModelAssembler,
  private val pageModelAssembler: PagedResourcesAssembler<ContentStorage>,
  private val enabledFeaturesProvider: EnabledFeaturesProvider
) {
  @PostMapping("")
  @Operation(description = "Create Content Storage")
  @RequiresProjectPermissions([Scope.CONTENT_DELIVERY_MANAGE])
  @AllowApiAccess
  fun create(@Valid @RequestBody dto: ContentStorageRequest): ContentStorageModel {
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationId = projectHolder.project.organizationOwnerId,
      Feature.PROJECT_LEVEL_CONTENT_STORAGES
    )
    val contentStorage = contentStorageService.create(projectHolder.project.id, dto)
    return contentStorageModelAssembler.toModel(contentStorage)
  }

  @PutMapping("/{contentDeliveryConfigId}")
  @Operation(description = "Updates Content Storage")
  @RequiresProjectPermissions([Scope.CONTENT_DELIVERY_MANAGE])
  @AllowApiAccess
  fun update(@PathVariable contentDeliveryConfigId: Long, @Valid @RequestBody dto: ContentStorageRequest): ContentStorageModel {
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationId = projectHolder.project.organizationOwnerId,
      Feature.PROJECT_LEVEL_CONTENT_STORAGES
    )
    val contentStorage = contentStorageService.update(projectHolder.project.id, contentDeliveryConfigId, dto)
    return contentStorageModelAssembler.toModel(contentStorage)
  }

  @RequiresProjectPermissions([Scope.CONTENT_DELIVERY_MANAGE])
  @GetMapping("")
  @Operation(description = "List existing Content Storages")
  @AllowApiAccess
  fun list(@ParameterObject pageable: Pageable): PagedModel<ContentStorageModel> {
    val page = contentStorageService.getAllInProject(projectHolder.project.id, pageable)
    return pageModelAssembler.toModel(page, contentStorageModelAssembler)
  }

  @RequiresProjectPermissions([Scope.CONTENT_DELIVERY_MANAGE])
  @DeleteMapping("/{contentDeliveryConfigId}")
  @Operation(description = "Delete Content Storage")
  @AllowApiAccess
  fun delete(@PathVariable contentDeliveryConfigId: Long) {
    contentStorageService.delete(projectHolder.project.id, contentDeliveryConfigId)
  }

  @RequiresProjectPermissions([Scope.CONTENT_DELIVERY_MANAGE])
  @GetMapping("/{contentDeliveryConfigId}")
  @Operation(description = "Get Content Storage")
  @AllowApiAccess
  fun get(@PathVariable contentDeliveryConfigId: Long): ContentStorageModel {
    return contentStorageModelAssembler.toModel(contentStorageService.get(projectHolder.project.id, contentDeliveryConfigId))
  }

  @RequiresProjectPermissions([Scope.CONTENT_DELIVERY_MANAGE])
  @PostMapping("/test")
  @Operation(description = "Test Content Storage")
  @AllowApiAccess
  fun test(@Valid @RequestBody dto: ContentStorageRequest): StorageTestResult {
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationId = projectHolder.project.organizationOwnerId,
      Feature.PROJECT_LEVEL_CONTENT_STORAGES
    )
    return contentStorageService.testStorage(dto)
  }

  @RequiresProjectPermissions([Scope.CONTENT_DELIVERY_MANAGE])
  @PostMapping("/{id}/test")
  @Operation(description = "Tests existing Content Storage with new configuration." +
    " (Uses existing secrets, if nulls provided)")
  @AllowApiAccess
  fun testExisting(@Valid @RequestBody dto: ContentStorageRequest, @PathVariable id: Long): StorageTestResult {
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationId = projectHolder.project.organizationOwnerId,
      Feature.PROJECT_LEVEL_CONTENT_STORAGES
    )
    return contentStorageService.testStorage(dto, id)
  }
}
