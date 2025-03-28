package io.tolgee.ee.api.v2.controllers.glossary

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.ee.api.v2.hateoas.assemblers.glossary.GlossaryModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.glossary.GlossaryModel
import io.tolgee.ee.data.glossary.CreateGlossaryRequest
import io.tolgee.ee.data.glossary.UpdateGlossaryRequest
import io.tolgee.ee.service.glossary.GlossaryService
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.glossary.Glossary
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
@RequestMapping("/v2/organizations/{organizationId:[0-9]+}/glossaries")
@Tag(name = "Glossary")
class GlossaryController(
  private val glossaryService: GlossaryService,
  private val glossaryModelAssembler: GlossaryModelAssembler,
  private val pagedAssembler: PagedResourcesAssembler<Glossary>,
  private val organizationHolder: OrganizationHolder,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
) {
  @PostMapping
  @Transactional
  @Operation(summary = "Create glossary")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequiresOrganizationRole(OrganizationRoleType.OWNER) // TODO special role for glossaries
  fun create(
    @PathVariable
    organizationId: Long,
    @RequestBody @Valid
    dto: CreateGlossaryRequest,
  ): GlossaryModel {
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationHolder.organization.id,
      Feature.GLOSSARY,
    )

    val glossary = glossaryService.create(organizationHolder.organizationEntity, dto)
    return glossaryModelAssembler.toModel(glossary)
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update glossary")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequiresOrganizationRole(OrganizationRoleType.OWNER) // TODO special role for glossaries
  fun update(
    @PathVariable
    organizationId: Long,
    @PathVariable
    id: Long,
    @RequestBody @Valid
    dto: UpdateGlossaryRequest,
  ): GlossaryModel {
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationHolder.organization.id,
      Feature.GLOSSARY,
    )

    val organization = organizationHolder.organization
    val glossary = glossaryService.update(organization.id, id, dto)
    return glossaryModelAssembler.toModel(glossary)
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete glossary")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequiresOrganizationRole(OrganizationRoleType.OWNER) // TODO special role for glossaries
  fun delete(
    @PathVariable
    organizationId: Long,
    @PathVariable
    id: Long,
  ) {
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationHolder.organization.id,
      Feature.GLOSSARY,
    )

    glossaryService.delete(organizationHolder.organization.id, id)
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get glossary")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @UseDefaultPermissions
  fun get(
    @PathVariable
    organizationId: Long,
    @PathVariable
    id: Long,
  ): GlossaryModel {
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationHolder.organization.id,
      Feature.GLOSSARY,
    )

    val organization = organizationHolder.organization
    val glossary = glossaryService.get(organization.id, id)
    return glossaryModelAssembler.toModel(glossary)
  }

  @GetMapping()
  @Operation(summary = "Get all organization glossaries")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @UseDefaultPermissions
  fun getAll(
    @PathVariable
    organizationId: Long,
    @ParameterObject pageable: Pageable,
    @RequestParam("search") search: String?,
  ): PagedModel<GlossaryModel> {
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationHolder.organization.id,
      Feature.GLOSSARY,
    )

    val organization = organizationHolder.organization
    val glossaries = glossaryService.findAllPaged(organization.id, pageable, search)
    return pagedAssembler.toModel(glossaries, glossaryModelAssembler)
  }
}
