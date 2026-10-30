package io.tolgee.api.v2.controllers.project

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.community.CommunityActivity
import io.tolgee.dtos.community.CommunityContributorStats
import io.tolgee.dtos.request.community.PeriodParams
import io.tolgee.hateoas.community.CommunityContributorModel
import io.tolgee.hateoas.community.CommunityContributorModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.contributor.CommunityStatsService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:[0-9]+}/community"])
@Tag(name = "Community stats", description = "Contribution counters of a community (public) project")
class CommunityStatsController(
  private val communityStatsService: CommunityStatsService,
  private val pagedResourcesAssembler: PagedResourcesAssembler<CommunityContributorStats>,
  private val communityContributorModelAssembler: CommunityContributorModelAssembler,
) {
  @GetMapping("/contributors")
  @Operation(
    summary = "Get community contributors",
    description =
      "Users who worked on translations in the project within the range: translating, changing translation " +
        "states, commenting, suggesting, and accepting or declining suggestions. For each user: the number of " +
        "contributions, suggestions, accepted and declined suggestions, the counts per language, and one bucket " +
        "of counts per day or week, all within the range. The dates of the first and last contribution are not " +
        "limited by the range or the languages: they cover the user's whole history in the project. " +
        DEFAULT_RANGE + PeriodParams.RANGE_LIMITS + " " + APPROXIMATE +
        "`active` is exact: the number of the user's suggestions that are pending right now. " +
        "Sort by contributions, suggested, accepted, declined, active or lastContributionAt; users without a " +
        "lastContributionAt come last. By default, most contributions come first, then most suggestions.",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun getContributors(
    @PathVariable projectId: Long,
    @ParameterObject pageable: Pageable,
    @ParameterObject period: PeriodParams,
    @Parameter(
      description =
        "Count only these languages. Users with no contributions in them within the range are left out. " +
          "The filter also applies to the totals, the buckets, the counts per language and `active`.",
    )
    @RequestParam(required = false) languageIds: List<Long>?,
    @Parameter(description = "Leave out contributors with no suggestion in the range.")
    @RequestParam(defaultValue = "false") excludeEmpty: Boolean,
  ): PagedModel<CommunityContributorModel> {
    val page = communityStatsService.getContributors(projectId, pageable, period, languageIds, excludeEmpty)
    return pagedResourcesAssembler.toModel(page, communityContributorModelAssembler)
  }

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
        DEFAULT_RANGE + PeriodParams.RANGE_LIMITS + " " + APPROXIMATE +
        "The two translation counts are exact: they are read from the suggestions as they are now.",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun getActivity(
    @PathVariable projectId: Long,
    @ParameterObject period: PeriodParams,
    @Parameter(description = "Count only these languages, in the buckets and in both translation counts.")
    @RequestParam(required = false) languageIds: List<Long>?,
  ): CommunityActivity = communityStatsService.getActivity(projectId, period, languageIds)

  companion object {
    private const val DEFAULT_RANGE =
      "By default, the range is the 12 weeks ending with the week of `to` (today, unless given), with one " +
        "bucket per week. "
    private const val APPROXIMATE =
      "The counts of contributions, suggestions, and accepted and declined suggestions are approximate: they " +
        "come from the activity log and never go down, so a suggestion made in one period and accepted or " +
        "declined in a later one counts in both. "
  }
}
