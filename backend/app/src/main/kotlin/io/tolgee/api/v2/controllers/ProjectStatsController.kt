/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.hateoas.project.stats.LanguageStatsModel
import io.tolgee.api.v2.hateoas.project.stats.ProjectStatsModel
import io.tolgee.constants.Message
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.views.projectStats.ProjectLanguageStatsResultView
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithAnyProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.ProjectService
import io.tolgee.service.ProjectStatsService
import org.springframework.hateoas.MediaTypes
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId}/stats", "/v2/projects/stats"])
@Tag(name = "Projects")
class ProjectStatsController(
  private val projectStatsService: ProjectStatsService,
  private val projectHolder: ProjectHolder,
  private val projectService: ProjectService
) {
  @Operation(summary = "Returns project stats")
  @GetMapping("", produces = [MediaTypes.HAL_JSON_VALUE])
  @AccessWithAnyProjectPermission
  @AccessWithApiKey
  fun getProjectStats(@PathVariable projectId: Long): ProjectStatsModel {
    val projectStats = projectStatsService.getProjectStats(projectId)
    val languageStats = projectStatsService.getLanguageStats(projectId)

    val baseLanguage = projectService.getOrCreateBaseLanguage(projectHolder.project.id)
    val baseStats = languageStats.find { it.languageId == baseLanguage?.id }
      ?: throw NotFoundException(Message.BASE_LANGUAGE_NOT_FOUND)

    val nonBaseLanguages = languageStats.filter { it.languageId != baseStats.languageId }
    val baseWordsCount = baseStats.translatedWords + baseStats.reviewedWords

    val allNonBaseTotalBaseWords = baseWordsCount * nonBaseLanguages.size
    val allNonBaseTotalTranslatedWords = nonBaseLanguages.sumOf { it.translatedWords }
    val allNonBaseTotalReviewedWords = nonBaseLanguages.sumOf { it.reviewedWords }

    val translatedPercent = (allNonBaseTotalTranslatedWords.toDouble() / allNonBaseTotalBaseWords) * 100
    val reviewedPercent = (allNonBaseTotalReviewedWords.toDouble() / allNonBaseTotalBaseWords) * 100

    return ProjectStatsModel(
      projectId = projectStats.id,
      languageCount = languageStats.size,
      keyCount = projectStats.keyCount,
      baseWordsCount = baseWordsCount,
      translatedPercentage = translatedPercent,
      reviewedPercentage = reviewedPercent,
      membersCount = projectStats.memberCount,
      tagCount = projectStats.tagCount,
      languageStats = getSortedLanguageStatModels(languageStats, baseStats)
    )
  }

  @Operation(summary = "Returns project daily amount of events")
  @GetMapping("/daily-activity", produces = [MediaTypes.HAL_JSON_VALUE])
  @AccessWithAnyProjectPermission
  @AccessWithApiKey
  fun getProjectDailyActivity(@PathVariable projectId: Long): Map<LocalDate, Long> {
    return projectStatsService.getProjectDailyActivity(projectId)
  }

  private fun getSortedLanguageStatModels(
    languageStats: List<ProjectLanguageStatsResultView>,
    baseStats: ProjectLanguageStatsResultView
  ) = languageStats.sortedBy { it.languageName }.sortedBy { it.languageId != baseStats.languageId }.map {
    LanguageStatsModel(
      languageId = it.languageId,
      languageTag = it.languageTag,
      languageName = it.languageName,
      languageOriginalName = it.languageOriginalName,
      languageFlagEmoji = it.languageFlagEmoji,
      translatedKeyCount = it.translatedKeys,
      translatedWordCount = it.translatedWords,
      translatedPercentage = it.translatedWords.toDouble() /
        (baseStats.translatedWords + baseStats.reviewedWords) * 100,
      reviewedKeyCount = it.reviewedKeys,
      reviewedWordCount = it.reviewedWords,
      reviewedPercentage = it.reviewedWords.toDouble() /
        (baseStats.translatedWords + baseStats.reviewedWords) * 100,
    )
  }
}
