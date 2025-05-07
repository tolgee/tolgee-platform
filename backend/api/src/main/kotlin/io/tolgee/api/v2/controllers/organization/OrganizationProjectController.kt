/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.organization

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.cacheable.OrganizationLanguageDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.facade.ProjectWithStatsFacade
import io.tolgee.hateoas.language.OrganizationLanguageModel
import io.tolgee.hateoas.language.OrganizationLanguageModelAssembler
import io.tolgee.hateoas.project.ProjectModel
import io.tolgee.hateoas.project.ProjectModelAssembler
import io.tolgee.hateoas.project.ProjectWithStatsModel
import io.tolgee.model.views.ProjectWithLanguagesView
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.language.LanguageService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.data.web.SortDefault
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/organizations"])
@Tag(name = "Organizations")
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class OrganizationProjectController(
  private val organizationService: OrganizationService,
  private val pagedProjectResourcesAssembler: PagedResourcesAssembler<ProjectWithLanguagesView>,
  private val projectService: ProjectService,
  private val projectModelAssembler: ProjectModelAssembler,
  private val projectWithStatsFacade: ProjectWithStatsFacade,
  private val languageService: LanguageService,
  private val organizationLanguageModelAssembler: OrganizationLanguageModelAssembler,
  private val pagedOrganizationLanguageAssembler: PagedResourcesAssembler<OrganizationLanguageDto>,
) {
  @GetMapping("/{id:[0-9]+}/projects")
  @Operation(
    summary = "Get all accessible projects (by ID)",
    description = "Returns all organization projects the user has access to",
  )
  @UseDefaultPermissions
  fun getAllProjects(
    @PathVariable("id") id: Long,
    @ParameterObject pageable: Pageable,
    @RequestParam("search") search: String?,
  ): PagedModel<ProjectModel> {
    return organizationService.find(id)?.let { organization ->
      projectService.findPermittedInOrganizationPaged(pageable, search, organizationId = organization.id)
        .let { projects ->
          pagedProjectResourcesAssembler.toModel(projects, projectModelAssembler)
        }
    } ?: throw NotFoundException()
  }

  @GetMapping("/{slug:.*[a-z].*}/projects")
  @Operation(
    summary = "Get all accessible projects (by slug)",
    description = "Returns all organization projects the user has access to",
  )
  @UseDefaultPermissions
  fun getAllProjects(
    @PathVariable("slug") slug: String,
    @ParameterObject pageable: Pageable,
    @RequestParam("search") search: String?,
  ): PagedModel<ProjectModel> {
    return organizationService.find(slug)?.let {
      getAllProjects(it.id, pageable, search)
    } ?: throw NotFoundException()
  }

  @Operation(
    summary = "Get all projects with stats",
    description =
      "Returns all projects (including statistics)" +
        " where current user has any permission (except none)",
  )
  @GetMapping("/{organizationId:[0-9]+}/projects-with-stats")
  @UseDefaultPermissions
  fun getAllWithStatistics(
    @ParameterObject pageable: Pageable,
    @RequestParam("search") search: String?,
    @PathVariable organizationId: Long,
  ): PagedModel<ProjectWithStatsModel> {
    val projects = projectService.findPermittedInOrganizationPaged(pageable, search, organizationId = organizationId)
    return projectWithStatsFacade.getPagedModelWithStats(projects)
  }

  @Operation(
    summary = "Get all projects with stats",
    description =
      "Returns all projects (including statistics) " +
        "where current user has any permission (except none)",
  )
  @GetMapping("/{slug:.*[a-z].*}/projects-with-stats")
  @UseDefaultPermissions
  fun getAllWithStatistics(
    @ParameterObject
    @SortDefault("id")
    pageable: Pageable,
    @RequestParam("search") search: String?,
    @PathVariable("slug") organizationSlug: String,
  ): PagedModel<ProjectWithStatsModel> {
    return organizationService.findDto(organizationSlug)?.let { organization ->
      return getAllWithStatistics(pageable, search, organization.id)
    } ?: throw NotFoundException()
  }

  @Operation(
    summary = "Get all languages in use by projects owned by specified organization",
    description = "Returns all languages in use by projects owned by specified organization",
  )
  @GetMapping("/{organizationId:[0-9]+}/languages")
  @UseDefaultPermissions
  fun getAllLanguagesInUse(
    @ParameterObject
    @SortDefault("base", direction = Sort.Direction.DESC)
    @SortDefault("tag", direction = Sort.Direction.ASC)
    pageable: Pageable,
    @RequestParam("search") search: String?,
    @PathVariable organizationId: Long,
  ): PagedModel<OrganizationLanguageModel> {
    val languages = languageService.getPagedByOrganization(organizationId, pageable, search)
    return pagedOrganizationLanguageAssembler.toModel(languages, organizationLanguageModelAssembler)
  }
}
