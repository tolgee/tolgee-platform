package io.tolgee.ee.api.v2.controllers.glossary

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Feature
import io.tolgee.ee.api.v2.hateoas.assemblers.glossary.GlossaryTermTranslationModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.glossary.GlossaryTermTranslationModel
import io.tolgee.ee.data.glossary.UpdateGlossaryTermTranslationRequest
import io.tolgee.ee.service.glossary.GlossaryTermService
import io.tolgee.ee.service.glossary.GlossaryTermTranslationService
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.security.authorization.UseDefaultPermissions
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
  "/v2/organizations/{organizationId:[0-9]+}/glossaries/{glossaryId:[0-9]+}/terms/{termId:[0-9]+}/translations",
)
@Tag(name = "Glossary term translations")
class GlossaryTermTranslationController(
  private val glossaryTermService: GlossaryTermService,
  private val glossaryTermTranslationService: GlossaryTermTranslationService,
  private val modelAssembler: GlossaryTermTranslationModelAssembler,
) {
  @PostMapping()
  @Operation(summary = "Set a new glossary term translation for language")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequiresOrganizationRole(OrganizationRoleType.MAINTAINER)
  @RequiresFeatures(Feature.GLOSSARY)
  @Transactional
  @RequestActivity(ActivityType.GLOSSARY_TERM_TRANSLATION_UPDATE)
  fun update(
    @PathVariable
    organizationId: Long,
    @PathVariable
    glossaryId: Long,
    @PathVariable
    termId: Long,
    @RequestBody
    dto: UpdateGlossaryTermTranslationRequest,
  ): GlossaryTermTranslationModel {
    val glossaryTerm = glossaryTermService.get(organizationId, glossaryId, termId)
    val translation = glossaryTermTranslationService.updateOrCreate(glossaryTerm, dto)
    return translation?.let { modelAssembler.toModel(translation) } ?: GlossaryTermTranslationModel.defaultValue(
      dto.languageTag,
    )
  }

  @GetMapping("/{languageTag}")
  @Operation(summary = "Get glossary term translation for language")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @UseDefaultPermissions
  @RequiresFeatures(Feature.GLOSSARY)
  fun get(
    @PathVariable
    organizationId: Long,
    @PathVariable
    glossaryId: Long,
    @PathVariable
    termId: Long,
    @PathVariable
    languageTag: String,
  ): GlossaryTermTranslationModel {
    val glossaryTerm = glossaryTermService.get(organizationId, glossaryId, termId)
    val translation = glossaryTermTranslationService.find(glossaryTerm, languageTag)
    return translation?.let { modelAssembler.toModel(translation) } ?: GlossaryTermTranslationModel.defaultValue(
      languageTag,
    )
  }
}
