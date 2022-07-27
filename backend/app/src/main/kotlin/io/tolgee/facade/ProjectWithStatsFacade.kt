package io.tolgee.facade

import io.tolgee.api.v2.hateoas.project.ProjectWithStatsModel
import io.tolgee.api.v2.hateoas.project.ProjectWithStatsModelAssembler
import io.tolgee.dtos.query_results.ProjectStatistics
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.views.ProjectWithLanguagesView
import io.tolgee.model.views.ProjectWithStatsView
import io.tolgee.service.project.ProjectService
import io.tolgee.service.project.ProjectStatsService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

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
    val totals = projectStatsService.getProjectsTotals(projectIds)
    val languages = projectService.getProjectsWithFetchedLanguages(projectIds)
      .associate { it.id to it.languages.toList() }

    val languageStats = projectStatsService.getLanguageStats(projectIds)

    val projectsWithStatsContent = projects.content.map {
      val projectTotals = totals[it.id]
      val baseLanguage = it.baseLanguage
      val projectLanguageStats = languageStats[it.id]

      var stateTotals: ProjectStatsService.ProjectStateTotals? = null
      if (baseLanguage != null && projectLanguageStats != null) {
        stateTotals = projectStatsService.computeProjectTotals(baseLanguage, projectLanguageStats)
      }
      val translatedPercent = stateTotals?.translatedPercent.toPercentValue()
      val reviewedPercent = stateTotals?.reviewedPercent.toPercentValue()
      val untranslatedPercent = (BigDecimal(100) - translatedPercent - reviewedPercent).setScale(
        2,
        RoundingMode.HALF_UP
      )

      val projectStatistics = ProjectStatistics(
        projectId = it.id,
        keyCount = projectTotals?.keyCount ?: 0,
        languageCount = projectTotals?.languageCount ?: 0,
        translationStatePercentages = mapOf(
          TranslationState.TRANSLATED to translatedPercent,
          TranslationState.REVIEWED to reviewedPercent,
          TranslationState.UNTRANSLATED to untranslatedPercent
        )
      )
      ProjectWithStatsView(it, projectStatistics, languages[it.id]!!)
    }
    val page = PageImpl(projectsWithStatsContent, projects.pageable, projects.totalElements)
    return pagedWithStatsResourcesAssembler.toModel(page, projectWithStatsModelAssembler)
  }

  fun Double?.toPercentValue(): BigDecimal {
    if (this == null || this.isNaN()) {
      return BigDecimal(0)
    }
    return this.toBigDecimal().setScale(3, RoundingMode.HALF_UP)
  }
}
