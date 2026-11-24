package io.tolgee.api.v2.controllers

import io.tolgee.api.mcp.McpFacade
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.queryResults.LanguageStatsDto
import io.tolgee.security.ProjectHolder
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.LanguageStatsService
import org.springframework.stereotype.Service

@Service
class ProjectStatsFacade(
  private val projectHolder: ProjectHolder,
  private val languageService: LanguageService,
  private val languageStatsService: LanguageStatsService,
) : McpFacade {
  fun getLanguageStats(): List<Pair<LanguageDto?, LanguageStatsDto>> {
    val languages = languageService.findAll(projectHolder.project.id)
    val stats = languageStatsService.getLanguageStats(projectHolder.project.id, languages.map { it.id }.toSet())
    val languageById = languages.associateBy { it.id }
    return stats.map { languageById[it.languageId] to it }
  }
}
