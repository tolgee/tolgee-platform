package io.tolgee.ee.api.v2.controllers.glossary

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.component.reporting.BusinessEventPublisher
import io.tolgee.component.reporting.OnBusinessEventToCaptureEvent
import io.tolgee.constants.Feature
import io.tolgee.ee.api.v2.hateoas.assemblers.glossary.GlossaryModelAssembler
import io.tolgee.ee.api.v2.hateoas.assemblers.glossary.SimpleGlossaryModelAssembler
import io.tolgee.ee.api.v2.hateoas.assemblers.glossary.SimpleGlossaryWithStatsModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.glossary.GlossaryModel
import io.tolgee.ee.api.v2.hateoas.model.glossary.SimpleGlossaryModel
import io.tolgee.ee.api.v2.hateoas.model.glossary.SimpleGlossaryWithStatsModel
import io.tolgee.ee.data.glossary.CreateGlossaryRequest
import io.tolgee.ee.data.glossary.GlossaryWithStats
import io.tolgee.ee.data.glossary.UpdateGlossaryRequest
import io.tolgee.ee.service.glossary.GlossaryService
import io.tolgee.hateoas.project.SimpleProjectModel
import io.tolgee.hateoas.project.SimpleProjectModelAssembler
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.glossary.Glossary
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.security.authorization.UseDefaultPermissions
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.CollectionModel
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
@RequestMapping("/v2/organizations/{organizationId:[0-9]+}")
@Tag(name = "Glossary")
class GlossaryController(
  private val glossaryService: GlossaryService,
  private val glossaryModelAssembler: GlossaryModelAssembler,
  private val simpleGlossaryModelAssembler: SimpleGlossaryModelAssembler,
  private val simpleGlossaryWithStatsModelAssembler: SimpleGlossaryWithStatsModelAssembler,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val pagedAssembler: PagedResourcesAssembler<Glossary>,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val pagedWithStatsAssembler: PagedResourcesAssembler<GlossaryWithStats>,
  private val simpleProjectModelAssembler: SimpleProjectModelAssembler,
  private val organizationHolder: OrganizationHolder,
  private val businessEventPublisher: BusinessEventPublisher,
  private val authenticationFacade: AuthenticationFacade,
) {
  @PostMapping("/glossaries")
  @Operation(summary = "Create glossary")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequiresOrganizationRole(OrganizationRoleType.MAINTAINER)
  @RequestActivity(ActivityType.GLOSSARY_CREATE)
  @RequiresFeatures(Feature.GLOSSARY)
  @Transactional
  fun create(
    @PathVariable
    organizationId: Long,
    @RequestBody @Valid
    dto: CreateGlossaryRequest,
  ): GlossaryModel {
    val glossary = glossaryService.create(organizationHolder.organizationEntity, dto)
    return glossaryModelAssembler.toModel(glossary)
  }

  @PutMapping("/glossaries/{glossaryId:[0-9]+}")
  @Operation(summary = "Update glossary")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequiresOrganizationRole(OrganizationRoleType.MAINTAINER)
  @RequestActivity(ActivityType.GLOSSARY_UPDATE)
  @RequiresFeatures(Feature.GLOSSARY)
  @Transactional
  fun update(
    @PathVariable
    organizationId: Long,
    @PathVariable
    glossaryId: Long,
    @RequestBody @Valid
    dto: UpdateGlossaryRequest,
  ): GlossaryModel {
    val organization = organizationHolder.organization
    val glossary = glossaryService.update(organization.id, glossaryId, dto)
    return glossaryModelAssembler.toModel(glossary)
  }

  @DeleteMapping("/glossaries/{glossaryId:[0-9]+}")
  @Operation(summary = "Delete glossary")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @RequiresOrganizationRole(OrganizationRoleType.MAINTAINER)
  @RequestActivity(ActivityType.GLOSSARY_DELETE)
  @RequiresFeatures(Feature.GLOSSARY)
  @Transactional
  fun delete(
    @PathVariable
    organizationId: Long,
    @PathVariable
    glossaryId: Long,
  ) {
    businessEventPublisher.publish(
      OnBusinessEventToCaptureEvent(
        eventName = "GLOSSARY_DELETE",
        userAccountDto = authenticationFacade.authenticatedUser,
      ),
    )

    glossaryService.delete(organizationHolder.organization.id, glossaryId)
  }

  @GetMapping("/glossaries/{glossaryId:[0-9]+}")
  @Operation(summary = "Get glossary")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @UseDefaultPermissions
  @RequiresFeatures(Feature.GLOSSARY)
  fun get(
    @PathVariable
    organizationId: Long,
    @PathVariable
    glossaryId: Long,
  ): GlossaryModel {
    val organization = organizationHolder.organization
    val glossary = glossaryService.get(organization.id, glossaryId)
    return glossaryModelAssembler.toModel(glossary)
  }

  @GetMapping("/glossaries")
  @Operation(summary = "Get all organization glossaries")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @UseDefaultPermissions
  @RequiresFeatures(Feature.GLOSSARY)
  fun getAll(
    @PathVariable
    organizationId: Long,
    @ParameterObject pageable: Pageable,
    @RequestParam("search", required = false) search: String?,
  ): PagedModel<SimpleGlossaryModel> {
    val organization = organizationHolder.organization
    val glossaries = glossaryService.findAllPaged(organization.id, pageable, search)
    return pagedAssembler.toModel(glossaries, simpleGlossaryModelAssembler)
  }

  @GetMapping("/glossaries-with-stats")
  @Operation(summary = "Get all organization glossaries with some additional statistics")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @UseDefaultPermissions
  @RequiresFeatures(Feature.GLOSSARY)
  fun getAllWithStats(
    @PathVariable
    organizationId: Long,
    @ParameterObject pageable: Pageable,
    @RequestParam("search") search: String?,
  ): PagedModel<SimpleGlossaryWithStatsModel> {
    val organization = organizationHolder.organization
    val glossaries = glossaryService.findAllWithStatsPaged(organization.id, pageable, search)
    return pagedWithStatsAssembler.toModel(glossaries, simpleGlossaryWithStatsModelAssembler)
  }

  @GetMapping("/glossaries/{glossaryId:[0-9]+}/assigned-projects")
  @Operation(summary = "Get all projects assigned to glossary")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @UseDefaultPermissions
  @RequiresFeatures(Feature.GLOSSARY)
  fun getAssignedProjects(
    @PathVariable
    organizationId: Long,
    @PathVariable
    glossaryId: Long,
  ): CollectionModel<SimpleProjectModel> {
    val organization = organizationHolder.organization
    val glossary = glossaryService.get(organization.id, glossaryId)
    return simpleProjectModelAssembler.toCollectionModel(glossary.assignedProjects)
  }
}
