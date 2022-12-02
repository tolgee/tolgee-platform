/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.organization

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.hateoas.project.ProjectModel
import io.tolgee.api.v2.hateoas.project.ProjectModelAssembler
import io.tolgee.api.v2.hateoas.project.ProjectWithStatsModel
import io.tolgee.exceptions.NotFoundException
import io.tolgee.facade.ProjectWithStatsFacade
import io.tolgee.model.views.ProjectWithLanguagesView
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import org.springdoc.api.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.data.web.SortDefault
import org.springframework.hateoas.MediaTypes
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
  private val projectWithStatsFacade: ProjectWithStatsFacade
) {
  @GetMapping("/{id:[0-9]+}/projects")
  @Operation(summary = "Returns all organization projects")
  fun getAllProjects(
    @PathVariable("id") id: Long,
    @ParameterObject pageable: Pageable,
    @RequestParam("search") search: String?
  ): PagedModel<ProjectModel> {
    return organizationService.find(id)?.let {
      projectService.findAllInOrganization(it.id, pageable, search).let { projects ->
        pagedProjectResourcesAssembler.toModel(projects, projectModelAssembler)
      }
    } ?: throw NotFoundException()
  }

  @GetMapping("/{slug:.*[a-z].*}/projects")
  @Operation(summary = "Returns all organization projects")
  fun getAllProjects(
    @PathVariable("slug") slug: String,
    @ParameterObject pageable: Pageable,
    @RequestParam("search") search: String?
  ): PagedModel<ProjectModel> {
    return organizationService.find(slug)?.let {
      getAllProjects(it.id, pageable, search)
    } ?: throw NotFoundException()
  }

  @Operation(summary = "Returns all projects (including statistics) where current user has any permission")
  @GetMapping("/{organizationId:[0-9]+}/projects-with-stats", produces = [MediaTypes.HAL_JSON_VALUE])
  fun getAllWithStatistics(
    @ParameterObject pageable: Pageable,
    @RequestParam("search") search: String?,
    @PathVariable organizationId: Long
  ): PagedModel<ProjectWithStatsModel> {
    val projects = projectService.findPermittedPaged(pageable, search, organizationId = organizationId)
    return projectWithStatsFacade.getPagedModelWithStats(projects)
  }

  @Operation(summary = "Returns all projects (including statistics) where current user has any permission")
  @GetMapping("/{slug:.*[a-z].*}/projects-with-stats", produces = [MediaTypes.HAL_JSON_VALUE])
  fun getAllWithStatistics(
    @ParameterObject @SortDefault("id") pageable: Pageable,
    @RequestParam("search") search: String?,
    @PathVariable("slug") organizationSlug: String
  ): PagedModel<ProjectWithStatsModel> {
    return organizationService.find(organizationSlug)?.let { organization ->
      return getAllWithStatistics(pageable, search, organization.id)
    } ?: throw NotFoundException()
  }
}
