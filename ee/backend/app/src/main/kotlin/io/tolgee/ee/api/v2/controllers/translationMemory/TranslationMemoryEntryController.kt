package io.tolgee.ee.api.v2.controllers.translationMemory

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Feature
import io.tolgee.ee.api.v2.hateoas.assemblers.translationMemory.TranslationMemoryEntryGroupModelAssembler
import io.tolgee.ee.api.v2.hateoas.assemblers.translationMemory.TranslationMemoryEntryModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.translationMemory.TranslationMemoryEntryGroupModel
import io.tolgee.ee.api.v2.hateoas.model.translationMemory.TranslationMemoryEntryModel
import io.tolgee.ee.data.translationMemory.CreateTranslationMemoryEntryRequest
import io.tolgee.ee.data.translationMemory.DeleteMultipleTranslationMemoryEntriesRequest
import io.tolgee.ee.data.translationMemory.UpdateTranslationMemoryEntryRequest
import io.tolgee.ee.service.translationMemory.TranslationMemoryEntryGroup
import io.tolgee.ee.service.translationMemory.TranslationMemoryEntryManagementService
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.security.authorization.UseDefaultPermissions
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
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
@RequestMapping("/v2/organizations/{organizationId:[0-9]+}/translation-memories/{translationMemoryId:[0-9]+}/entries")
@Tag(name = "Translation Memory")
class TranslationMemoryEntryController(
  private val translationMemoryEntryManagementService: TranslationMemoryEntryManagementService,
  private val translationMemoryEntryModelAssembler: TranslationMemoryEntryModelAssembler,
  private val translationMemoryEntryGroupModelAssembler: TranslationMemoryEntryGroupModelAssembler,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val pagedGroupAssembler: PagedResourcesAssembler<TranslationMemoryEntryGroup>,
) {
  @GetMapping
  @Operation(
    summary = "List entries of a translation memory grouped by source text (paginated)",
    description =
      "One row per distinct source text. The `targetLanguageTag` filter only narrows which " +
        "entries are returned per group — source rows with no matching translation still appear " +
        "with an empty `entries` list so the user can add a translation.",
  )
  @UseDefaultPermissions
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequiresFeatures(Feature.TRANSLATION_MEMORY)
  fun list(
    @PathVariable organizationId: Long,
    @PathVariable translationMemoryId: Long,
    @RequestParam(required = false) search: String?,
    @RequestParam(required = false) targetLanguageTag: String?,
    @ParameterObject pageable: Pageable,
  ): PagedModel<TranslationMemoryEntryGroupModel> {
    val page =
      translationMemoryEntryManagementService.listEntryGroups(
        organizationId = organizationId,
        translationMemoryId = translationMemoryId,
        pageable = pageable,
        search = search,
        targetLanguageTag = targetLanguageTag,
      )
    return pagedGroupAssembler.toModel(page, translationMemoryEntryGroupModelAssembler)
  }

  @GetMapping("/{entryId:[0-9]+}")
  @Operation(summary = "Get a single translation memory entry")
  @UseDefaultPermissions
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequiresFeatures(Feature.TRANSLATION_MEMORY)
  fun get(
    @PathVariable organizationId: Long,
    @PathVariable translationMemoryId: Long,
    @PathVariable entryId: Long,
  ): TranslationMemoryEntryModel {
    val entry = translationMemoryEntryManagementService.getEntry(organizationId, translationMemoryId, entryId)
    return translationMemoryEntryModelAssembler.toModel(entry)
  }

  @PostMapping
  @Operation(summary = "Create a translation memory entry")
  @RequiresOrganizationRole(OrganizationRoleType.MAINTAINER)
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequestActivity(ActivityType.TRANSLATION_MEMORY_ENTRY_CREATE)
  @RequiresFeatures(Feature.TRANSLATION_MEMORY)
  @Transactional
  fun create(
    @PathVariable organizationId: Long,
    @PathVariable translationMemoryId: Long,
    @RequestBody @Valid dto: CreateTranslationMemoryEntryRequest,
  ): TranslationMemoryEntryModel {
    val entry = translationMemoryEntryManagementService.create(organizationId, translationMemoryId, dto)
    return translationMemoryEntryModelAssembler.toModel(entry)
  }

  @PutMapping("/{entryId:[0-9]+}")
  @Operation(summary = "Update a translation memory entry")
  @RequiresOrganizationRole(OrganizationRoleType.MAINTAINER)
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequestActivity(ActivityType.TRANSLATION_MEMORY_ENTRY_UPDATE)
  @RequiresFeatures(Feature.TRANSLATION_MEMORY)
  @Transactional
  fun update(
    @PathVariable organizationId: Long,
    @PathVariable translationMemoryId: Long,
    @PathVariable entryId: Long,
    @RequestBody @Valid dto: UpdateTranslationMemoryEntryRequest,
  ): TranslationMemoryEntryModel {
    val entry = translationMemoryEntryManagementService.update(organizationId, translationMemoryId, entryId, dto)
    return translationMemoryEntryModelAssembler.toModel(entry)
  }

  @DeleteMapping("/{entryId:[0-9]+}")
  @Operation(summary = "Delete a translation memory entry")
  @RequiresOrganizationRole(OrganizationRoleType.MAINTAINER)
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequestActivity(ActivityType.TRANSLATION_MEMORY_ENTRY_DELETE)
  @RequiresFeatures(Feature.TRANSLATION_MEMORY)
  @Transactional
  fun delete(
    @PathVariable organizationId: Long,
    @PathVariable translationMemoryId: Long,
    @PathVariable entryId: Long,
  ) {
    translationMemoryEntryManagementService.delete(organizationId, translationMemoryId, entryId)
  }

  @DeleteMapping
  @Operation(
    summary = "Batch delete translation memory entry groups",
    description =
      "For every entry ID in the payload, deletes the entire group that shares the same " +
        "source text (and key). The request is deduplicated to distinct groups so passing " +
        "multiple entries from the same row is a no-op past the first one.",
  )
  @RequiresOrganizationRole(OrganizationRoleType.MAINTAINER)
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequestActivity(ActivityType.TRANSLATION_MEMORY_ENTRY_DELETE)
  @RequiresFeatures(Feature.TRANSLATION_MEMORY)
  @Transactional
  fun deleteMultipleGroups(
    @PathVariable organizationId: Long,
    @PathVariable translationMemoryId: Long,
    @RequestBody @Valid dto: DeleteMultipleTranslationMemoryEntriesRequest,
  ) {
    translationMemoryEntryManagementService.deleteMultipleGroups(
      organizationId = organizationId,
      translationMemoryId = translationMemoryId,
      entryIds = dto.entryIds,
    )
  }

  @DeleteMapping("/{entryId:[0-9]+}/group")
  @Operation(
    summary = "Delete a whole translation memory entry group",
    description =
      "Deletes every entry that shares the same source text (and key) as the given " +
        "entry — i.e. the entire translation-unit group visible as one row in the UI.",
  )
  @RequiresOrganizationRole(OrganizationRoleType.MAINTAINER)
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequestActivity(ActivityType.TRANSLATION_MEMORY_ENTRY_DELETE)
  @RequiresFeatures(Feature.TRANSLATION_MEMORY)
  @Transactional
  fun deleteGroup(
    @PathVariable organizationId: Long,
    @PathVariable translationMemoryId: Long,
    @PathVariable entryId: Long,
  ) {
    translationMemoryEntryManagementService.deleteGroup(
      organizationId = organizationId,
      translationMemoryId = translationMemoryId,
      entryId = entryId,
    )
  }
}
