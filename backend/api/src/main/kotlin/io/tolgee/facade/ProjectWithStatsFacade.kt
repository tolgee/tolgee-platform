package io.tolgee.facade

import io.sentry.Sentry
import io.tolgee.dtos.queryResults.ProjectStatistics
import io.tolgee.hateoas.project.ProjectWithStatsModel
import io.tolgee.hateoas.project.ProjectWithStatsModelAssembler
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.views.ProjectWithLanguagesView
import io.tolgee.model.views.ProjectWithStatsView
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.LanguageStatsService
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
  private val languageStatsService: LanguageStatsService,
  private val languageService: LanguageService,
) {
  fun getPagedModelWithStats(projects: Page<ProjectWithLanguagesView>): PagedModel<ProjectWithStatsModel> {
    val projectIds = projects.content.map { it.id }
    val totals = projectStatsService.getProjectsTotals(projectIds)
    val languages = languageService.getDtosOfProjects(projectIds)
    val languageStats = languageStatsService.getLanguageStatsDtos(projectIds)

    val projectsWithStatsContent =
      projects.content.map { projectWithLanguagesView ->
        val projectTotals = totals[projectWithLanguagesView.id]
        val baseLanguage = languages[projectWithLanguagesView.id]?.find { it.base }
        val projectLanguageStats =
          languageStats[projectWithLanguagesView.id]

        var stateTotals: ProjectStatsService.ProjectStateTotals? = null
        if (baseLanguage != null && projectLanguageStats != null) {
          stateTotals =
            projectStatsService.computeProjectTotals(
              baseLanguage,
              projectLanguageStats,
            )
        }

        val translatedPercent = stateTotals?.translatedPercent.toPercentValue()
        val reviewedPercent = stateTotals?.reviewedPercent.toPercentValue()
        val untranslatedPercent =
          (BigDecimal(100) - translatedPercent - reviewedPercent).setScale(
            2,
            RoundingMode.HALF_UP,
          )

        val projectStatistics =
          ProjectStatistics(
            projectId = projectWithLanguagesView.id,
            keyCount = projectTotals?.keyCount ?: 0,
            languageCount = projectTotals?.languageCount ?: 0,
            translationStatePercentages =
              mapOf(
                TranslationState.TRANSLATED to translatedPercent,
                TranslationState.REVIEWED to reviewedPercent,
                TranslationState.UNTRANSLATED to untranslatedPercent,
              ),
          )

        val projectLanguages =
          languages[projectWithLanguagesView.id]
            ?.sortedBy { it.name }
            ?.sortedBy { it.id != baseLanguage?.id } ?: listOf()

        ProjectWithStatsView(
          view = projectWithLanguagesView,
          stats = projectStatistics,
          languages = projectLanguages,
        )
      }
    val page = PageImpl(projectsWithStatsContent, projects.pageable, projects.totalElements)
    return pagedWithStatsResourcesAssembler.toModel(page, projectWithStatsModelAssembler)
  }

  fun Double?.toPercentValue(): BigDecimal {
    if (this == null || this.isNaN()) {
      return BigDecimal(0)
    }
    return try {
      this.toBigDecimal().setScale(3, RoundingMode.HALF_UP)
    } catch (e: NumberFormatException) {
      Sentry.captureMessage("Failed to convert $this to BigDecimal")
      Sentry.captureException(e)
      BigDecimal(0)
    }
  }
}
