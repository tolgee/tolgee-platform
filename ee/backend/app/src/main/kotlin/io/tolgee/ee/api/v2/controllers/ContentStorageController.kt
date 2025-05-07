package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.dtos.contentDelivery.ContentStorageRequest
import io.tolgee.ee.api.v2.hateoas.assemblers.ContentStorageModelAssemblerEeImpl
import io.tolgee.ee.data.StorageTestResult
import io.tolgee.ee.service.ContentStorageService
import io.tolgee.hateoas.ee.contentStorage.ContentStorageModel
import io.tolgee.model.contentDelivery.ContentStorage
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiEeExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.*

@Suppress("MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId:[0-9]+}/content-storages",
  ],
)
@Tag(name = "Content Storages")
@OpenApiEeExtension
class ContentStorageController(
  private val contentStorageService: ContentStorageService,
  private val projectHolder: ProjectHolder,
  private val contentStorageModelAssemblerEeImpl: ContentStorageModelAssemblerEeImpl,
  private val pageModelAssembler: PagedResourcesAssembler<ContentStorage>,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
) {
  @PostMapping("")
  @Operation(summary = "Create Content Storage")
  @RequiresProjectPermissions([Scope.CONTENT_DELIVERY_MANAGE])
  @RequestActivity(ActivityType.CONTENT_STORAGE_CREATE)
  @AllowApiAccess
  fun create(
    @Valid @RequestBody
    dto: ContentStorageRequest,
  ): ContentStorageModel {
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationId = projectHolder.project.organizationOwnerId,
      Feature.PROJECT_LEVEL_CONTENT_STORAGES,
    )
    val contentStorage = contentStorageService.create(projectHolder.project.id, dto)
    return contentStorageModelAssemblerEeImpl.toModel(contentStorage)
  }

  @PutMapping("/{contentStorageId}")
  @Operation(summary = "Update Content Storage")
  @RequiresProjectPermissions([Scope.CONTENT_DELIVERY_MANAGE])
  @AllowApiAccess
  @RequestActivity(ActivityType.CONTENT_STORAGE_UPDATE)
  fun update(
    @PathVariable contentStorageId: Long,
    @Valid @RequestBody
    dto: ContentStorageRequest,
  ): ContentStorageModel {
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationId = projectHolder.project.organizationOwnerId,
      Feature.PROJECT_LEVEL_CONTENT_STORAGES,
    )
    val contentStorage = contentStorageService.update(projectHolder.project.id, contentStorageId, dto)
    return contentStorageModelAssemblerEeImpl.toModel(contentStorage)
  }

  @RequiresProjectPermissions([Scope.CONTENT_DELIVERY_MANAGE])
  @GetMapping("")
  @Operation(summary = "List Content Storages")
  @AllowApiAccess
  fun list(
    @ParameterObject pageable: Pageable,
  ): PagedModel<ContentStorageModel> {
    val page = contentStorageService.getAllInProject(projectHolder.project.id, pageable)
    return pageModelAssembler.toModel(page, contentStorageModelAssemblerEeImpl)
  }

  @RequiresProjectPermissions([Scope.CONTENT_DELIVERY_MANAGE])
  @DeleteMapping("/{contentStorageId}")
  @Operation(summary = "Delete Content Storage")
  @RequestActivity(ActivityType.CONTENT_STORAGE_DELETE)
  @AllowApiAccess
  fun delete(
    @PathVariable contentStorageId: Long,
  ) {
    contentStorageService.delete(projectHolder.project.id, contentStorageId)
  }

  @RequiresProjectPermissions([Scope.CONTENT_DELIVERY_MANAGE])
  @GetMapping("/{contentStorageId}")
  @Operation(summary = "Get Content Storage")
  @AllowApiAccess
  fun get(
    @PathVariable contentStorageId: Long,
  ): ContentStorageModel {
    return contentStorageModelAssemblerEeImpl
      .toModel(contentStorageService.get(projectHolder.project.id, contentStorageId))
  }

  @RequiresProjectPermissions([Scope.CONTENT_DELIVERY_MANAGE])
  @PostMapping("/test")
  @Operation(summary = "Test Content Storage settings")
  @AllowApiAccess
  fun test(
    @Valid @RequestBody
    dto: ContentStorageRequest,
  ): StorageTestResult {
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationId = projectHolder.project.organizationOwnerId,
      Feature.PROJECT_LEVEL_CONTENT_STORAGES,
    )
    return contentStorageService.testStorage(dto)
  }

  @RequiresProjectPermissions([Scope.CONTENT_DELIVERY_MANAGE])
  @PostMapping("/{id}/test")
  @Operation(
    summary = "Test existing Content Storage",
    description =
      "Tests existing Content Storage with new configuration." +
        " (Uses existing secrets, if nulls provided)",
  )
  @AllowApiAccess
  fun testExisting(
    @Valid @RequestBody
    dto: ContentStorageRequest,
    @PathVariable id: Long,
  ): StorageTestResult {
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationId = projectHolder.project.organizationOwnerId,
      Feature.PROJECT_LEVEL_CONTENT_STORAGES,
    )
    return contentStorageService.testStorage(dto, id)
  }
}
