package io.tolgee.facade

import io.tolgee.api.v2.hateoas.project.ProjectWithStatsModel
import io.tolgee.api.v2.hateoas.project.ProjectWithStatsModelAssembler
import io.tolgee.model.views.ProjectWithLanguagesView
import io.tolgee.model.views.ProjectWithStatsView
import io.tolgee.service.project.ProjectService
import io.tolgee.service.project.ProjectStatsService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.stereotype.Component

@Component
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class ProjectWithStatsFacade(
  private val projectStatsService: ProjectStatsService,
  private val pagedWithStatsResourcesAssembler: PagedResourcesAssembler<ProjectWithStatsView>,
  private val projectWithStatsModelAssembler: ProjectWithStatsModelAssembler,
  private val projectService: ProjectService
) {
  fun getPagedModelWithStats(
    projects: Page<ProjectWithLanguagesView>,
  ): PagedModel<ProjectWithStatsModel> {
    val projectIds = projects.content.map { it.id }
    val stats = projectStatsService.getProjectsTotals(projectIds).associateBy { it.projectId }
    val languages = projectService.getProjectsWithFetchedLanguages(projectIds)
      .associateTo(linkedMapOf()) { it.id to it.languages.toList() }
    val projectsWithStatsContent = projects.content.map { project ->
      val projectLanguages = (languages[project.id] ?: listOf())
        .sortedBy { it.name }
        .sortedBy { it.id != project.baseLanguage?.id }
      ProjectWithStatsView(project, stats[project.id]!!, projectLanguages)
    }
    val page = PageImpl(projectsWithStatsContent, projects.pageable, projects.totalElements)
    return pagedWithStatsResourcesAssembler.toModel(page, projectWithStatsModelAssembler)
  }
}
