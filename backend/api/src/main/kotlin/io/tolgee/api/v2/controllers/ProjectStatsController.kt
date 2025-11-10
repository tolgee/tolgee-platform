/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.hateoas.project.stats.LanguageStatsModelAssembler
import io.tolgee.hateoas.project.stats.ProjectStatsModel
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.LanguageStatsService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.project.ProjectStatsService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:[0-9]+}/stats", "/v2/projects/stats"])
@Tag(name = "Project Stats")
class ProjectStatsController(
  private val projectStatsService: ProjectStatsService,
  private val projectHolder: ProjectHolder,
  private val projectService: ProjectService,
  private val languageStatsService: LanguageStatsService,
  private val languageStatsModelAssembler: LanguageStatsModelAssembler,
  private val languageService: LanguageService,
) {
  @Operation(summary = "Get project stats")
  @GetMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
  @UseDefaultPermissions
  @AllowApiAccess
  fun getProjectStats(): ProjectStatsModel {
    val projectStats = projectStatsService.getProjectStats(projectHolder.project.id)
    val baseLanguage = projectService.getOrAssignBaseLanguage(projectHolder.project.id)
    val languages = languageService.getProjectLanguages(projectHolder.project.id).associateBy { it.id }
    val languageStats =
      languageStatsService
        .getLanguageStats(projectHolder.project.id)
        .sortedBy { languages[it.languageId]?.name }
        .sortedBy { languages[it.languageId]?.base == false }

    val totals = projectStatsService.computeProjectTotals(baseLanguage, languageStats)

    val statsLanguagePairs =
      languageStats.mapNotNull {
        val language = languages[it.languageId] ?: return@mapNotNull null
        it to language
      }

    return ProjectStatsModel(
      projectId = projectStats.id,
      languageCount = languageStats.size,
      keyCount = projectStats.keyCount,
      taskCount = projectStats.taskCount,
      baseWordsCount = totals.baseWordsCount,
      translatedPercentage = totals.translatedPercent,
      reviewedPercentage = totals.reviewedPercent,
      membersCount = projectStats.memberCount,
      tagCount = projectStats.tagCount,
      languageStats = statsLanguagePairs.map { languageStatsModelAssembler.toModel(it) },
    )
  }

  @Operation(summary = "Get project daily amount of events")
  @GetMapping("/daily-activity")
  @RequiresProjectPermissions([Scope.ACTIVITY_VIEW])
  @AllowApiAccess
  fun getProjectDailyActivity(): Map<LocalDate, Long> {
    return projectStatsService.getProjectDailyActivity(projectHolder.project.id)
  }
}
