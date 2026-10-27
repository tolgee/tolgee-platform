package io.tolgee.api.v2.controllers.project

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Message
import io.tolgee.dtos.community.CommunityActivity
import io.tolgee.dtos.community.CommunityContributorStats
import io.tolgee.exceptions.NotFoundException
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
      "Users with translation contributions in the project (translations, state changes, comments, " +
        "suggestions and their moderation), with contributions / suggested / accepted / declined totals, " +
        "first and last contribution dates, per-language counts and weekly buckets for the last `weeks` " +
        "ISO weeks (1 to 104). Sortable by contributions, suggested, accepted and declined; " +
        "default contributions desc, suggested desc.",
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

  @GetMapping("/contributors/{userId:[0-9]+}")
  @Operation(
    summary = "Get one community contributor",
    description =
      "Stats of one contributor of this project, in the shape of a contributors list item. " +
        "Returns 404 when the user has no contributions here, or the account is deleted or disabled.",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun getContributor(
    @PathVariable projectId: Long,
    @PathVariable userId: Long,
    @RequestParam(defaultValue = CommunityStatsService.DEFAULT_WEEKS.toString()) weeks: Int,
  ): CommunityContributorModel {
    val stats =
      communityStatsService.getContributor(projectId, userId, weeks)
        ?: throw NotFoundException(Message.USER_NOT_FOUND)
    return communityContributorModelAssembler.toModel(stats)
  }

  @GetMapping("/activity")
  @Operation(
    summary = "Get community activity",
    description =
      "Contributions / suggested / accepted / declined counts per ISO week for the last `weeks` weeks (1 to 104), " +
        "oldest first, zero-filled.",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun getActivity(
    @PathVariable projectId: Long,
    @RequestParam(defaultValue = CommunityStatsService.DEFAULT_WEEKS.toString()) weeks: Int,
  ): CommunityActivity = communityStatsService.getActivity(projectId, weeks)
}
