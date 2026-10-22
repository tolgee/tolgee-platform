package io.tolgee.api.v2.controllers.project

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.community.CommunityActivity
import io.tolgee.dtos.community.CommunityContributorStats
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
      "Users who suggested translations in the project, with suggested / accepted / declined totals, " +
        "per-language counts and weekly buckets for the last `weeks` ISO weeks (1 to 104). " +
        "Sortable by suggested, accepted and declined; default accepted desc, suggested desc.",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun getContributors(
    @PathVariable projectId: Long,
    @ParameterObject pageable: Pageable,
    @RequestParam(defaultValue = CommunityStatsService.DEFAULT_WEEKS.toString()) weeks: Int,
  ): PagedModel<CommunityContributorModel> {
    val page = communityStatsService.getContributors(projectId, pageable, weeks)
    return pagedResourcesAssembler.toModel(page, communityContributorModelAssembler)
  }

  @GetMapping("/activity")
  @Operation(
    summary = "Get community activity",
    description =
      "Suggested / accepted / declined counts per ISO week for the last `weeks` weeks (1 to 104), " +
        "oldest first, zero-filled.",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun getActivity(
    @PathVariable projectId: Long,
    @RequestParam(defaultValue = CommunityStatsService.DEFAULT_WEEKS.toString()) weeks: Int,
  ): CommunityActivity = communityStatsService.getActivity(projectId, weeks)
}
