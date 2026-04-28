package io.tolgee.ee.api.v2.controllers.translationMemory

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Feature
import io.tolgee.ee.api.v2.hateoas.assemblers.translationMemory.SimpleTranslationMemoryModelAssembler
import io.tolgee.ee.api.v2.hateoas.assemblers.translationMemory.TmAssignedProjectModelAssembler
import io.tolgee.ee.api.v2.hateoas.assemblers.translationMemory.TranslationMemoryModelAssembler
import io.tolgee.ee.api.v2.hateoas.assemblers.translationMemory.TranslationMemoryWithStatsModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.translationMemory.SimpleTranslationMemoryModel
import io.tolgee.ee.api.v2.hateoas.model.translationMemory.TmAssignedProjectModel
import io.tolgee.ee.api.v2.hateoas.model.translationMemory.TranslationMemoryModel
import io.tolgee.ee.api.v2.hateoas.model.translationMemory.TranslationMemoryWithStatsModel
import io.tolgee.ee.data.translationMemory.CreateSharedTranslationMemoryRequest
import io.tolgee.ee.data.translationMemory.UpdateProjectTmSettingsRequest
import io.tolgee.ee.data.translationMemory.UpdateSharedTranslationMemoryRequest
import io.tolgee.ee.service.translationMemory.SharedTranslationMemoryService
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.translationMemory.TranslationMemory
import io.tolgee.model.translationMemory.TranslationMemoryWithStats
import io.tolgee.repository.translationMemory.TranslationMemoryProjectRepository
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.security.authorization.UseDefaultPermissions
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.PagedModel
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/organizations/{organizationId:[0-9]+}")
@Tag(name = "Translation Memory")
class SharedTranslationMemoryController(
  private val sharedTranslationMemoryService: SharedTranslationMemoryService,
  private val translationMemoryModelAssembler: TranslationMemoryModelAssembler,
  private val simpleTranslationMemoryModelAssembler: SimpleTranslationMemoryModelAssembler,
  private val translationMemoryWithStatsModelAssembler: TranslationMemoryWithStatsModelAssembler,
  private val tmAssignedProjectModelAssembler: TmAssignedProjectModelAssembler,
  private val translationMemoryProjectRepository: TranslationMemoryProjectRepository,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val pagedAssembler: PagedResourcesAssembler<TranslationMemory>,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val pagedWithStatsAssembler: PagedResourcesAssembler<TranslationMemoryWithStats>,
  private val organizationHolder: OrganizationHolder,
) {
  @PostMapping("/translation-memories")
  @Operation(summary = "Create shared translation memory")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequiresOrganizationRole(OrganizationRoleType.MAINTAINER)
  @RequestActivity(ActivityType.TRANSLATION_MEMORY_CREATE)
  @RequiresFeatures(Feature.TRANSLATION_MEMORY)
  @Transactional
  fun create(
    @PathVariable organizationId: Long,
    @RequestBody @Valid dto: CreateSharedTranslationMemoryRequest,
  ): TranslationMemoryModel {
    val tm = sharedTranslationMemoryService.create(organizationHolder.organizationEntity, dto)
    return translationMemoryModelAssembler.toModel(tm)
  }

  @PutMapping("/translation-memories/{translationMemoryId:[0-9]+}/write-only-reviewed")
  @Operation(
    summary = "Toggle the reviewed-only flag on any TM in the organization",
    description =
      "Sets `writeOnlyReviewed` on the given TM. Unlike the main update endpoint, this accepts " +
        "PROJECT-type TMs too, so org maintainers can edit this single setting from the org-level " +
        "TM list without switching into project settings.",
  )
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequiresOrganizationRole(OrganizationRoleType.MAINTAINER)
  @RequestActivity(ActivityType.TRANSLATION_MEMORY_UPDATE)
  @RequiresFeatures(Feature.TRANSLATION_MEMORY)
  @Transactional
  fun setWriteOnlyReviewed(
    @PathVariable organizationId: Long,
    @PathVariable translationMemoryId: Long,
    @RequestBody @Valid dto: UpdateProjectTmSettingsRequest,
  ) {
    sharedTranslationMemoryService.setWriteOnlyReviewed(
      organizationHolder.organization.id,
      translationMemoryId,
      dto.writeOnlyReviewed,
    )
  }

  @PutMapping("/translation-memories/{translationMemoryId:[0-9]+}")
  @Operation(summary = "Update shared translation memory")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequiresOrganizationRole(OrganizationRoleType.MAINTAINER)
  @RequestActivity(ActivityType.TRANSLATION_MEMORY_UPDATE)
  @RequiresFeatures(Feature.TRANSLATION_MEMORY)
  @Transactional
  fun update(
    @PathVariable organizationId: Long,
    @PathVariable translationMemoryId: Long,
    @RequestBody @Valid dto: UpdateSharedTranslationMemoryRequest,
  ): TranslationMemoryModel {
    val tm = sharedTranslationMemoryService.update(organizationHolder.organization.id, translationMemoryId, dto)
    return translationMemoryModelAssembler.toModel(tm)
  }

  @DeleteMapping("/translation-memories/{translationMemoryId:[0-9]+}")
  @Operation(summary = "Delete shared translation memory")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequiresOrganizationRole(OrganizationRoleType.MAINTAINER)
  @RequestActivity(ActivityType.TRANSLATION_MEMORY_DELETE)
  @RequiresFeatures(Feature.TRANSLATION_MEMORY)
  @Transactional
  fun delete(
    @PathVariable organizationId: Long,
    @PathVariable translationMemoryId: Long,
  ) {
    sharedTranslationMemoryService.delete(organizationHolder.organization.id, translationMemoryId)
  }

  @GetMapping("/translation-memories/{translationMemoryId:[0-9]+}")
  @Operation(summary = "Get translation memory")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @UseDefaultPermissions
  @RequiresFeatures(Feature.TRANSLATION_MEMORY)
  fun get(
    @PathVariable organizationId: Long,
    @PathVariable translationMemoryId: Long,
  ): TranslationMemoryModel {
    val tm = sharedTranslationMemoryService.get(organizationHolder.organization.id, translationMemoryId)
    return translationMemoryModelAssembler.toModel(tm)
  }

  @GetMapping("/translation-memories")
  @Operation(summary = "Get all translation memories in the organization")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @UseDefaultPermissions
  @RequiresFeatures(Feature.TRANSLATION_MEMORY)
  fun getAll(
    @PathVariable organizationId: Long,
    @ParameterObject pageable: Pageable,
    @RequestParam("search", required = false) search: String?,
  ): PagedModel<SimpleTranslationMemoryModel> {
    val page = sharedTranslationMemoryService.findAllPaged(organizationHolder.organization.id, pageable, search)
    return pagedAssembler.toModel(page, simpleTranslationMemoryModelAssembler)
  }

  @GetMapping("/translation-memories-with-stats")
  @Operation(summary = "Get all translation memories with statistics")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @UseDefaultPermissions
  @RequiresFeatures(Feature.TRANSLATION_MEMORY)
  fun getAllWithStats(
    @PathVariable organizationId: Long,
    @ParameterObject pageable: Pageable,
    @RequestParam("search", required = false) search: String?,
    @RequestParam("type", required = false) type: String?,
  ): PagedModel<TranslationMemoryWithStatsModel> {
    val page =
      sharedTranslationMemoryService.findAllWithStatsPaged(organizationHolder.organization.id, pageable, search, type)
    return pagedWithStatsAssembler.toModel(page, translationMemoryWithStatsModelAssembler)
  }

  @GetMapping("/translation-memories/{translationMemoryId:[0-9]+}/assigned-projects")
  @Operation(summary = "Get projects assigned to a translation memory")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @UseDefaultPermissions
  @RequiresFeatures(Feature.TRANSLATION_MEMORY)
  fun getAssignedProjects(
    @PathVariable organizationId: Long,
    @PathVariable translationMemoryId: Long,
  ): CollectionModel<TmAssignedProjectModel> {
    sharedTranslationMemoryService.get(organizationHolder.organization.id, translationMemoryId)
    val assignments = translationMemoryProjectRepository.findByTranslationMemoryId(translationMemoryId)
    return tmAssignedProjectModelAssembler.toCollectionModel(assignments.sortedBy { it.priority })
  }
}
