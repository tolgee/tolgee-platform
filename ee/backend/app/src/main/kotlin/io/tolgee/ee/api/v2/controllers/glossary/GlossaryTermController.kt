package io.tolgee.ee.api.v2.controllers.glossary

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.ee.api.v2.hateoas.assemblers.glossary.GlossaryTermModelAssembler
import io.tolgee.ee.api.v2.hateoas.assemblers.glossary.GlossaryTermTranslationModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.glossary.GlossaryTermModel
import io.tolgee.ee.data.glossary.CreateGlossaryTermRequest
import io.tolgee.ee.data.glossary.CreateGlossaryTermResponse
import io.tolgee.ee.data.glossary.UpdateGlossaryTermRequest
import io.tolgee.ee.service.glossary.GlossaryTermService
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.glossary.GlossaryTerm
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.security.authorization.UseDefaultPermissions
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
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
@RequestMapping("/v2/organizations/{organizationId:[0-9]+}/glossaries/{glossaryId:[0-9]+}/terms")
@Tag(name = "Glossary Term")
class GlossaryTermController(
  private val glossaryTermService: GlossaryTermService,
  private val glossaryTermModelAssembler: GlossaryTermModelAssembler,
  private val glossaryTermTranslationModelAssembler: GlossaryTermTranslationModelAssembler,
  private val pagedAssembler: PagedResourcesAssembler<GlossaryTerm>,
) {
  @PostMapping()
  @Operation(summary = "Create a new glossary term")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequiresOrganizationRole(OrganizationRoleType.OWNER) // TODO special role for glossaries
  fun create(
    @PathVariable
    organizationId: Long,
    @PathVariable
    glossaryId: Long,
    @RequestBody
    dto: CreateGlossaryTermRequest,
  ): CreateGlossaryTermResponse {
    val (term, translation) = glossaryTermService.create(organizationId, glossaryId, dto)
    return CreateGlossaryTermResponse(
      term = glossaryTermModelAssembler.toModel(term),
      translation = glossaryTermTranslationModelAssembler.toModel(translation),
    )
  }

  @PutMapping("/{termId:[0-9]+}")
  @Operation(summary = "Update glossary term")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequiresOrganizationRole(OrganizationRoleType.OWNER) // TODO special role for glossaries
  fun update(
    @PathVariable organizationId: Long,
    @PathVariable glossaryId: Long,
    @PathVariable termId: Long,
    @RequestBody @Valid dto: UpdateGlossaryTermRequest,
  ): GlossaryTermModel {
    val updatedTerm = glossaryTermService.update(organizationId, glossaryId, termId, dto)
    return glossaryTermModelAssembler.toModel(updatedTerm)
  }

  @DeleteMapping("/{termId:[0-9]+}")
  @Operation(summary = "Delete glossary term")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequiresOrganizationRole(OrganizationRoleType.OWNER) // TODO special role for glossaries
  fun delete(
    @PathVariable organizationId: Long,
    @PathVariable glossaryId: Long,
    @PathVariable termId: Long,
  ) {
    glossaryTermService.delete(organizationId, glossaryId, termId)
  }

  @GetMapping("/{termId:[0-9]+}")
  @Operation(summary = "Get glossary term")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @UseDefaultPermissions
  fun get(
    @PathVariable organizationId: Long,
    @PathVariable glossaryId: Long,
    @PathVariable termId: Long,
  ): GlossaryTermModel {
    val glossaryTerm = glossaryTermService.get(organizationId, glossaryId, termId)
    return glossaryTermModelAssembler.toModel(glossaryTerm)
  }

  @GetMapping
  @Operation(summary = "Get all glossary terms")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @UseDefaultPermissions
  fun getAll(
    @PathVariable organizationId: Long,
    @PathVariable glossaryId: Long,
    @ParameterObject pageable: Pageable,
    @RequestParam("search", required = false) search: String?,
  ): PagedModel<GlossaryTermModel> {
    val terms = glossaryTermService.findAllPaged(organizationId, glossaryId, pageable, search)
    return pagedAssembler.toModel(terms, glossaryTermModelAssembler)
  }
}
