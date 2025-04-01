package io.tolgee.ee.api.v2.controllers.glossary

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.ee.api.v2.hateoas.assemblers.glossary.GlossaryTermModelAssembler
import io.tolgee.ee.api.v2.hateoas.assemblers.glossary.GlossaryTermTranslationModelAssembler
import io.tolgee.ee.data.glossary.CreateGlossaryTermRequest
import io.tolgee.ee.data.glossary.CreateGlossaryTermResponse
import io.tolgee.ee.data.glossary.GlossaryLanguageDto
import io.tolgee.ee.service.glossary.GlossaryService
import io.tolgee.ee.service.glossary.GlossaryTermService
import io.tolgee.ee.service.glossary.GlossaryTermTranslationService
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.security.authorization.UseDefaultPermissions
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/organizations/{organizationId:[0-9]+}/glossaries/{id:[0-9]+}")
@Tag(name = "Glossary Term")
class GlossaryTermController(
  private val glossaryService: GlossaryService,
  private val glossaryTermService: GlossaryTermService,
  private val glossaryTermTranslationService: GlossaryTermTranslationService,
  private val glossaryTermModelAssembler: GlossaryTermModelAssembler,
  private val glossaryTermTranslationModelAssembler: GlossaryTermTranslationModelAssembler,
) {
  @GetMapping("/languages")
  @Operation(summary = "Get all languages in use by the glossary")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @UseDefaultPermissions
  fun getLanguages(
    @PathVariable
    organizationId: Long,
    @PathVariable
    id: Long,
  ): List<GlossaryLanguageDto> {
    val glossary = glossaryService.get(organizationId, id)
    val languages = glossaryTermTranslationService.getDistinctLanguageTags(organizationId, id)
    return languages.map {
      GlossaryLanguageDto(it, glossary.baseLanguageCode == it)
    }
  }

  @PostMapping("/terms")
  @Operation(summary = "Create a new glossary term")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequiresOrganizationRole(OrganizationRoleType.OWNER) // TODO special role for glossaries
  fun createTerm(
    @PathVariable
    organizationId: Long,
    @PathVariable
    id: Long,
    @RequestBody
    dto: CreateGlossaryTermRequest,
  ): CreateGlossaryTermResponse {
    val (term, translation) = glossaryTermService.create(organizationId, id, dto)
    return CreateGlossaryTermResponse(
      term = glossaryTermModelAssembler.toModel(term),
      translation = glossaryTermTranslationModelAssembler.toModel(translation),
    )
  }
}
