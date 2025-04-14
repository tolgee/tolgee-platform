package io.tolgee.ee.api.v2.controllers.glossary

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.ee.api.v2.hateoas.assemblers.glossary.GlossaryTermModelAssembler
import io.tolgee.ee.api.v2.hateoas.assemblers.glossary.GlossaryTermTranslationModelAssembler
import io.tolgee.ee.api.v2.hateoas.assemblers.glossary.GlossaryTermWithTranslationsModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.glossary.GlossaryTermModel
import io.tolgee.ee.api.v2.hateoas.model.glossary.GlossaryTermWithTranslationsModel
import io.tolgee.ee.data.glossary.CreateGlossaryTermWithTranslationRequest
import io.tolgee.ee.data.glossary.CreateUpdateGlossaryTermResponse
import io.tolgee.ee.data.glossary.UpdateGlossaryTermWithTranslationRequest
import io.tolgee.ee.service.glossary.GlossaryTermService
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.glossary.GlossaryTerm
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.security.authorization.UseDefaultPermissions
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v2/organizations/{organizationId:[0-9]+}/glossaries/{glossaryId:[0-9]+}")
@Tag(name = "Glossary Term")
class GlossaryTermController(
  private val glossaryTermService: GlossaryTermService,
  private val glossaryTermModelAssembler: GlossaryTermModelAssembler,
  private val glossaryTermWithTranslationsModelAssembler: GlossaryTermWithTranslationsModelAssembler,
  private val glossaryTermTranslationModelAssembler: GlossaryTermTranslationModelAssembler,
  private val pagedAssembler: PagedResourcesAssembler<GlossaryTerm>,
  private val organizationHolder: OrganizationHolder,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
) {
  @PostMapping("/terms")
  @Operation(summary = "Create a new glossary term")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequiresOrganizationRole(OrganizationRoleType.OWNER) // TODO special role for glossaries
  fun create(
    @PathVariable
    organizationId: Long,
    @PathVariable
    glossaryId: Long,
    @RequestBody
    dto: CreateGlossaryTermWithTranslationRequest,
  ): CreateUpdateGlossaryTermResponse {
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationHolder.organization.id,
      Feature.GLOSSARY,
    )

    val (term, translation) = glossaryTermService.createWithTranslation(organizationId, glossaryId, dto)
    return CreateUpdateGlossaryTermResponse(
      term = glossaryTermModelAssembler.toModel(term),
      translation = translation?.let { glossaryTermTranslationModelAssembler.toModel(translation) },
    )
  }

  @PutMapping("/terms/{termId:[0-9]+}")
  @Operation(summary = "Update glossary term")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequiresOrganizationRole(OrganizationRoleType.OWNER) // TODO special role for glossaries
  fun update(
    @PathVariable organizationId: Long,
    @PathVariable glossaryId: Long,
    @PathVariable termId: Long,
    @RequestBody @Valid dto: UpdateGlossaryTermWithTranslationRequest,
  ): CreateUpdateGlossaryTermResponse {
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationHolder.organization.id,
      Feature.GLOSSARY,
    )

    val (term, translation) = glossaryTermService.updateWithTranslation(organizationId, glossaryId, termId, dto)
    return CreateUpdateGlossaryTermResponse(
      term = glossaryTermModelAssembler.toModel(term),
      translation = translation?.let { glossaryTermTranslationModelAssembler.toModel(translation) },
    )
  }

  @DeleteMapping("/terms/{termId:[0-9]+}")
  @Operation(summary = "Delete glossary term")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequiresOrganizationRole(OrganizationRoleType.OWNER) // TODO special role for glossaries
  fun delete(
    @PathVariable organizationId: Long,
    @PathVariable glossaryId: Long,
    @PathVariable termId: Long,
  ) {
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationHolder.organization.id,
      Feature.GLOSSARY,
    )

    glossaryTermService.delete(organizationId, glossaryId, termId)
  }

  @GetMapping("/terms/{termId:[0-9]+}")
  @Operation(summary = "Get glossary term")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @UseDefaultPermissions
  fun get(
    @PathVariable organizationId: Long,
    @PathVariable glossaryId: Long,
    @PathVariable termId: Long,
  ): GlossaryTermModel {
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationHolder.organization.id,
      Feature.GLOSSARY,
    )

    val glossaryTerm = glossaryTermService.get(organizationId, glossaryId, termId)
    return glossaryTermModelAssembler.toModel(glossaryTerm)
  }

  @GetMapping("/terms")
  @Operation(summary = "Get all glossary terms")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @UseDefaultPermissions
  fun getAll(
    @PathVariable organizationId: Long,
    @PathVariable glossaryId: Long,
    @ParameterObject pageable: Pageable,
    @RequestParam("search", required = false) search: String?,
    @RequestParam("languageTags", required = false) languageTags: List<String>?,
  ): PagedModel<GlossaryTermModel> {
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationHolder.organization.id,
      Feature.GLOSSARY,
    )

    val terms = glossaryTermService.findAllPaged(organizationId, glossaryId, pageable, search, languageTags?.toSet())
    return pagedAssembler.toModel(terms, glossaryTermModelAssembler)
  }

  @GetMapping("/termsWithTranslations")
  @Operation(summary = "Get all glossary terms with translations")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @UseDefaultPermissions
  fun getAllWithTranslations(
    @PathVariable organizationId: Long,
    @PathVariable glossaryId: Long,
    @ParameterObject pageable: Pageable,
    @RequestParam("search", required = false) search: String?,
    @RequestParam("languageTags", required = false) languageTags: List<String>?,
  ): PagedModel<GlossaryTermWithTranslationsModel> {
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationHolder.organization.id,
      Feature.GLOSSARY,
    )

    val terms =
      glossaryTermService.findAllPaged(
        organizationId,
        glossaryId,
        pageable,
        search,
        languageTags?.toSet(),
      )
    return pagedAssembler.toModel(terms, glossaryTermWithTranslationsModelAssembler)
  }
}
