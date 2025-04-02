package io.tolgee.ee.api.v2.controllers.glossary

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.ee.data.glossary.GlossaryLanguageDto
import io.tolgee.ee.service.glossary.GlossaryService
import io.tolgee.ee.service.glossary.GlossaryTermTranslationService
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authorization.UseDefaultPermissions
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/organizations/{organizationId:[0-9]+}/glossaries/{glossaryId:[0-9]+}/languages")
@Tag(name = "Glossary Languages")
class GlossaryLanguagesController(
  private val glossaryService: GlossaryService,
  private val glossaryTermTranslationService: GlossaryTermTranslationService,
) {
  @GetMapping()
  @Operation(summary = "Get all languages in use by the glossary")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @UseDefaultPermissions
  fun getLanguages(
    @PathVariable
    organizationId: Long,
    @PathVariable
    glossaryId: Long,
  ): List<GlossaryLanguageDto> {
    val glossary = glossaryService.get(organizationId, glossaryId)
    val languages = glossaryTermTranslationService.getDistinctLanguageTags(organizationId, glossaryId)
    return languages.map {
      GlossaryLanguageDto(it, glossary.baseLanguageCode == it)
    }
  }
}
