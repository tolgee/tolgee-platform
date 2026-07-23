package io.tolgee.api.v2.controllers.project

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.facade.ProjectWithStatsFacade
import io.tolgee.hateoas.project.ProjectWithStatsModel
import io.tolgee.openApiDocs.OpenApiHideFromPublicDocs
import io.tolgee.service.project.ProjectService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.SortDefault
import org.springframework.hateoas.PagedModel
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/public/projects"])
@Tag(name = "Public projects")
@OpenApiHideFromPublicDocs
class PublicProjectsController(
  private val projectService: ProjectService,
  private val projectWithStatsFacade: ProjectWithStatsFacade,
) : IController {
  @Operation(
    summary = "Get all public projects with stats",
    description =
      "Returns all public projects (including statistics), discoverable by anyone — no authentication required",
  )
  @GetMapping("/with-stats")
  @Transactional(readOnly = true)
  fun getAllPublicWithStatistics(
    @ParameterObject @SortDefault("name") pageable: Pageable,
    @RequestParam("search") search: String?,
    @RequestParam("filterContributed", defaultValue = "false") filterContributed: Boolean,
  ): PagedModel<ProjectWithStatsModel> {
    val projects = projectService.findAllPublicPaged(pageable, search, filterContributed)
    return projectWithStatsFacade.getPagedModelWithStats(projects)
  }
}
