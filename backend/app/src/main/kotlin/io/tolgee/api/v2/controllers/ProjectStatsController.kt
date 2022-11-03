/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.hateoas.project.stats.LanguageStatsModelAssembler
import io.tolgee.api.v2.hateoas.project.stats.ProjectStatsModel
import io.tolgee.security.apiKeyAuth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithAnyProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.project.LanguageStatsService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.project.ProjectStatsService
import org.springframework.hateoas.MediaTypes
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:[0-9]+}/stats", "/v2/projects/stats"])
@Tag(name = "Projects")
class ProjectStatsController(
  private val projectStatsService: ProjectStatsService,
  private val projectHolder: ProjectHolder,
  private val projectService: ProjectService,
  private val languageStatsService: LanguageStatsService,
  private val languageStatsModelAssembler: LanguageStatsModelAssembler
) {
  @Operation(summary = "Returns project stats")
  @GetMapping("", produces = [MediaTypes.HAL_JSON_VALUE])
  @AccessWithAnyProjectPermission
  @AccessWithApiKey
  fun getProjectStats(): ProjectStatsModel {
    val projectStats = projectStatsService.getProjectStats(projectHolder.project.id)
    val baseLanguage = projectService.getOrCreateBaseLanguage(projectHolder.project.id)
    val languageStats = languageStatsService.getLanguageStats(projectHolder.project.id)
      .sortedBy { it.language.name }
      .sortedBy { it.language.id != baseLanguage?.id }

    val totals = projectStatsService.computeProjectTotals(baseLanguage, languageStats)

    return ProjectStatsModel(
      projectId = projectStats.id,
      languageCount = languageStats.size,
      keyCount = projectStats.keyCount,
      baseWordsCount = totals.baseWordsCount,
      translatedPercentage = totals.translatedPercent,
      reviewedPercentage = totals.reviewedPercent,
      membersCount = projectStats.memberCount,
      tagCount = projectStats.tagCount,
      languageStats = languageStats.map { languageStatsModelAssembler.toModel(it) }
    )
  }

  @Operation(summary = "Returns project daily amount of events")
  @GetMapping("/daily-activity", produces = [MediaTypes.HAL_JSON_VALUE])
  @AccessWithAnyProjectPermission
  @AccessWithApiKey
  fun getProjectDailyActivity(): Map<LocalDate, Long> {
    return projectStatsService.getProjectDailyActivity(projectHolder.project.id)
  }
}
