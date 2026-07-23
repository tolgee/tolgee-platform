package io.tolgee.api.v2.controllers.project

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.hateoas.contributor.ContributorModel
import io.tolgee.hateoas.contributor.ContributorModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.model.views.ProjectContributorView
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.contributor.ProjectContributorService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects"])
@Tag(name = "Project Contributors")
class ProjectContributorsController(
  private val projectContributorService: ProjectContributorService,
  private val pagedResourcesAssembler: PagedResourcesAssembler<ProjectContributorView>,
  private val contributorModelAssembler: ContributorModelAssembler,
) {
  @GetMapping("/{projectId:[0-9]+}/contributors")
  @Operation(
    summary = "Get project contributors",
    description =
      "Returns users who have activity on the project but are not currently its members. " +
        "Deleted and disabled users are excluded. The response carries no email address.",
  )
  @RequiresProjectPermissions([Scope.MEMBERS_VIEW])
  @RequiresSuperAuthentication
  @AllowApiAccess
  fun getContributors(
    @PathVariable("projectId") projectId: Long,
    @ParameterObject pageable: Pageable,
  ): PagedModel<ContributorModel> {
    val contributors = projectContributorService.getContributors(projectId, pageable)
    return pagedResourcesAssembler.toModel(contributors, contributorModelAssembler)
  }
}
