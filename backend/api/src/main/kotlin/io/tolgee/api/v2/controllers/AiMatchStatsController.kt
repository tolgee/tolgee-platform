package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.aiMatchStats.AiMatchLangInfo
import io.tolgee.dtos.aiMatchStats.AiMatchLanguagesView
import io.tolgee.dtos.aiMatchStats.AiMatchPromptsView
import io.tolgee.dtos.aiMatchStats.AiMatchSummaryView
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.aiMatchStats.AiMatchStatsService
import io.tolgee.service.language.LanguageService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * AI-vs-reviewed match statistics: how much reviewers changed AI output before approving it.
 * Scoped to the default branch. The score range filter (`reviewedAfter`/`reviewedBefore`) is the
 * timestamp of the last `→ REVIEWED` transition.
 */
@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:[0-9]+}/ai-match-stats", "/v2/projects/ai-match-stats"])
@Tag(name = "AI Match Stats")
class AiMatchStatsController(
  private val projectHolder: ProjectHolder,
  private val languageService: LanguageService,
  private val aiMatchStatsService: AiMatchStatsService,
) {
  @Operation(summary = "Get AI match stats project summary")
  @GetMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun getSummary(
    @RequestParam(required = false) reviewedAfter: Long?,
    @RequestParam(required = false) reviewedBefore: Long?,
    @RequestParam(required = false) languages: List<String>?,
  ): AiMatchSummaryView =
    aiMatchStatsService.getSummary(projectHolder.project.id, resolveLanguages(languages), reviewedAfter, reviewedBefore)

  @Operation(summary = "Get AI match stats per language")
  @GetMapping("/languages", produces = [MediaType.APPLICATION_JSON_VALUE])
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun getLanguages(
    @RequestParam(required = false) reviewedAfter: Long?,
    @RequestParam(required = false) reviewedBefore: Long?,
    @RequestParam(required = false) languages: List<String>?,
  ): AiMatchLanguagesView =
    aiMatchStatsService.getLanguages(
      projectHolder.project.id,
      resolveLanguages(languages),
      reviewedAfter,
      reviewedBefore,
    )

  @Operation(summary = "Get AI match stats per prompt and prompt version")
  @GetMapping("/prompts", produces = [MediaType.APPLICATION_JSON_VALUE])
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun getPrompts(
    @RequestParam(required = false) reviewedAfter: Long?,
    @RequestParam(required = false) reviewedBefore: Long?,
    @RequestParam(required = false) languages: List<String>?,
  ): AiMatchPromptsView =
    aiMatchStatsService.getPrompts(projectHolder.project.id, resolveLanguages(languages), reviewedAfter, reviewedBefore)

  /** Non-base project languages, optionally narrowed to the requested tags. */
  private fun resolveLanguages(requestedTags: List<String>?): List<AiMatchLangInfo> {
    val wanted = requestedTags?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet()
    return languageService
      .getProjectLanguages(projectHolder.project.id)
      .filter { !it.base }
      .filter { wanted == null || it.tag in wanted }
      .map { AiMatchLangInfo(it.id, it.tag, it.name, it.flagEmoji) }
  }
}
