package io.tolgee.ee.api.v2.controllers.glossary

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Feature
import io.tolgee.ee.data.glossary.GlossaryImportResult
import io.tolgee.ee.service.glossary.GlossaryImportService
import io.tolgee.ee.service.glossary.GlossaryService
import io.tolgee.ee.service.glossary.GlossaryTermService
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresOrganizationRole
import org.springframework.http.MediaType
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/v2/organizations/{organizationId:[0-9]+}/glossaries/{glossaryId:[0-9]+}")
@Tag(name = "Glossary Import")
class GlossaryImportController(
  private val glossaryImportService: GlossaryImportService,
  private val glossaryService: GlossaryService,
  private val glossaryTermService: GlossaryTermService,
  private val organizationHolder: OrganizationHolder,
) {
  @PostMapping("/import", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @Operation(summary = "Import glossary terms from CSV")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequiresOrganizationRole(OrganizationRoleType.MAINTAINER)
  @RequiresFeatures(Feature.GLOSSARY)
  @RequestActivity(ActivityType.GLOSSARY_IMPORT)
  @Transactional
  fun importCsv(
    @PathVariable organizationId: Long,
    @PathVariable glossaryId: Long,
    @RequestPart("file") file: MultipartFile,
    @RequestParam(required = false, defaultValue = "false")
    removeExistingTerms: Boolean,
  ): GlossaryImportResult {
    val organization = organizationHolder.organization
    val glossary = glossaryService.get(organization.id, glossaryId)
    if (removeExistingTerms) {
      glossaryTermService.deleteAllByGlossary(glossary)
    }

    val imported =
      file.inputStream.use { input ->
        glossaryImportService.importCsv(glossary, input)
      }
    return GlossaryImportResult(imported)
  }
}
