package io.tolgee.ee.api.v2.controllers.translationMemory

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.ee.data.translationMemory.TmxImportResult
import io.tolgee.ee.service.translationMemory.SharedTranslationMemoryService
import io.tolgee.ee.service.translationMemory.TranslationMemoryTmxService
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.security.authorization.UseDefaultPermissions
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/v2/organizations/{organizationId:[0-9]+}/translation-memories/{translationMemoryId:[0-9]+}")
@Tag(name = "Translation Memory")
class TranslationMemoryTmxController(
  private val sharedTranslationMemoryService: SharedTranslationMemoryService,
  private val translationMemoryTmxService: TranslationMemoryTmxService,
  private val organizationHolder: OrganizationHolder,
) {
  @PostMapping("/import", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @Operation(summary = "Import TMX file into translation memory")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequiresOrganizationRole(OrganizationRoleType.MAINTAINER)
  @RequestActivity(ActivityType.TRANSLATION_MEMORY_IMPORT)
  @RequiresFeatures(Feature.TRANSLATION_MEMORY)
  @Transactional
  fun importTmx(
    @PathVariable organizationId: Long,
    @PathVariable translationMemoryId: Long,
    @RequestPart("file") file: MultipartFile,
    @RequestParam(defaultValue = "false") overrideExisting: Boolean,
  ): TmxImportResult {
    val tm = sharedTranslationMemoryService.get(organizationHolder.organization.id, translationMemoryId)
    try {
      return translationMemoryTmxService.importTmx(tm, file.inputStream, overrideExisting)
    } catch (e: Exception) {
      if (e is BadRequestException) throw e
      throw BadRequestException(Message.FILE_PROCESSING_FAILED)
    }
  }

  @GetMapping("/export")
  @Operation(summary = "Export translation memory as TMX file")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @UseDefaultPermissions
  @RequiresFeatures(Feature.TRANSLATION_MEMORY)
  fun exportTmx(
    @PathVariable organizationId: Long,
    @PathVariable translationMemoryId: Long,
  ): ResponseEntity<ByteArray> {
    val tm = sharedTranslationMemoryService.get(organizationHolder.organization.id, translationMemoryId)
    val filename = "tm-${tm.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.tmx"
    val bytes = translationMemoryTmxService.exportTmx(tm)

    return ResponseEntity
      .ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
      .contentType(MediaType.APPLICATION_XML)
      .body(bytes)
  }
}
