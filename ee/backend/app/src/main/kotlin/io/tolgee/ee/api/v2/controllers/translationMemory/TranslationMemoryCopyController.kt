package io.tolgee.ee.api.v2.controllers.translationMemory

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Feature
import io.tolgee.ee.data.translationMemory.CopyFromProjectRequest
import io.tolgee.ee.data.translationMemory.CopyFromProjectResult
import io.tolgee.ee.service.translationMemory.TranslationMemoryEntryManagementService
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresOrganizationRole
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/organizations/{organizationId:[0-9]+}/translation-memories/{translationMemoryId:[0-9]+}")
@Tag(name = "Translation Memory")
class TranslationMemoryCopyController(
  private val translationMemoryEntryManagementService: TranslationMemoryEntryManagementService,
) {
  @PostMapping("/copy-from-project")
  @Operation(
    summary = "Copy entries from a project's translation memory",
    description =
      "Seeds this translation memory with manual entries copied from the source project's TM " +
        "(both stored manual rows and virtual rows derived from the project's translations). " +
        "Skips duplicates by `(sourceText, targetLanguageTag, targetText)` so the operation is " +
        "idempotent and safe to run on a non-empty TM.",
  )
  @RequiresOrganizationRole(OrganizationRoleType.MAINTAINER)
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequestActivity(ActivityType.TRANSLATION_MEMORY_COPY_FROM_PROJECT)
  @RequiresFeatures(Feature.TRANSLATION_MEMORY)
  fun copyFromProject(
    @PathVariable organizationId: Long,
    @PathVariable translationMemoryId: Long,
    @RequestBody @Valid dto: CopyFromProjectRequest,
  ): CopyFromProjectResult {
    return translationMemoryEntryManagementService.copyFromProject(
      organizationId = organizationId,
      translationMemoryId = translationMemoryId,
      sourceProjectId = dto.sourceProjectId,
    )
  }
}
