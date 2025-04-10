package io.tolgee.ee.api.v2.controllers.glossary

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.ee.api.v2.hateoas.assemblers.glossary.GlossaryTermTranslationModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.glossary.GlossaryTermTranslationModel
import io.tolgee.ee.data.glossary.CreateGlossaryTermTranslationRequest
import io.tolgee.ee.service.glossary.GlossaryTermService
import io.tolgee.ee.service.glossary.GlossaryTermTranslationService
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authorization.RequiresOrganizationRole
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(
  "/v2/organizations/{organizationId:[0-9]+}/glossaries/{glossaryId:[0-9]+}/terms/{termId:[0-9]+}/translations",
)
@Tag(name = "Glossary Term Translations")
class GlossaryTermTranslationController(
  private val glossaryTermService: GlossaryTermService,
  private val glossaryTermTranslationService: GlossaryTermTranslationService,
  private val modelAssembler: GlossaryTermTranslationModelAssembler,
) {
  @PostMapping()
  @Operation(summary = "Set a new glossary term translation for language")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequiresOrganizationRole(OrganizationRoleType.OWNER) // TODO special role for glossaries
  fun update(
    @PathVariable
    organizationId: Long,
    @PathVariable
    glossaryId: Long,
    @PathVariable
    termId: Long,
    @RequestBody
    dto: CreateGlossaryTermTranslationRequest,
  ): GlossaryTermTranslationModel? {
    val glossaryTerm = glossaryTermService.get(organizationId, glossaryId, termId)
    val translation = glossaryTermTranslationService.updateOrCreate(glossaryTerm, dto)
    return translation?.let { modelAssembler.toModel(translation) }
  }
}
