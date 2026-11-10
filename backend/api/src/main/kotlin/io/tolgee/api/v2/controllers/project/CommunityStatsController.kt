package io.tolgee.api.v2.controllers.project

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.community.PeriodParams
import io.tolgee.dtos.request.community.RangeParams
import io.tolgee.hateoas.community.CommunityActivityStatsModel
import io.tolgee.hateoas.community.CommunityActivityStatsModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.contributor.CommunityStatsService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:[0-9]+}/community"])
@Tag(name = "Community stats", description = "Contribution counters of a project")
class CommunityStatsController(
  private val communityStatsService: CommunityStatsService,
  private val communityActivityStatsModelAssembler: CommunityActivityStatsModelAssembler,
) {
  @GetMapping("/activity")
  @Operation(
    summary = "Get community activity",
    description =
      "Counts of contributions, suggestions, and accepted and declined suggestions for each day or week of " +
        "the range, oldest first, with zeros where nothing happened. " +
        "Also two exact counts of translations, where a translation is one key in one language: " +
        "`translationsWithAnySuggestion` counts the translations that received a suggestion within the range, " +
        "and `translationsWithPendingSuggestion` counts those of them that still have a pending suggestion. " +
        "A pending suggestion created before the range is not counted. " +
        DEFAULT_RANGE + "By default there is one bucket per week. " + RangeParams.RANGE_LIMITS + " " + APPROXIMATE +
        "The two translation counts are exact: they are read from the suggestions as they are now.",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun getActivity(
    @PathVariable projectId: Long,
    @ParameterObject period: PeriodParams,
    @Parameter(description = "Count only these languages, in the buckets and in both translation counts.")
    @RequestParam(required = false) languageIds: List<Long>?,
  ): CommunityActivityStatsModel =
    communityActivityStatsModelAssembler.toModel(communityStatsService.getActivity(projectId, period, languageIds))

  companion object {
    private const val DEFAULT_RANGE =
      "By default, the range is the 12 weeks ending with the week of `to` (today, unless given). "
    private const val APPROXIMATE =
      "The counts of contributions, suggestions, and accepted and declined suggestions are approximate: they " +
        "come from the activity log and never go down, so a suggestion made in one period and accepted or " +
        "declined in a later one counts in both. "
  }
}
