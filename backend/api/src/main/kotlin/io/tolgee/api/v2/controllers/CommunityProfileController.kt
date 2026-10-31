package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.community.CommunityActivityItem
import io.tolgee.dtos.community.CommunityProfile
import io.tolgee.dtos.request.community.PeriodParams
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.community.CommunityActivityModel
import io.tolgee.hateoas.community.CommunityActivityModelAssembler
import io.tolgee.security.authentication.AllowGuestAccess
import io.tolgee.security.ratelimit.RateLimitService
import io.tolgee.service.contributor.CommunityStatsService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/users/{userId:[0-9]+}"])
@Tag(
  name = "Community profile",
  description = "A user's contributions to public projects, readable with or without an account",
)
class CommunityProfileController(
  private val communityStatsService: CommunityStatsService,
  private val pagedResourcesAssembler: PagedResourcesAssembler<CommunityActivityItem>,
  private val communityActivityModelAssembler: CommunityActivityModelAssembler,
  private val rateLimitService: RateLimitService,
  private val tolgeeProperties: TolgeeProperties,
) {
  @GetMapping("/community-profile")
  @AllowGuestAccess
  @Operation(
    summary = "Get community profile",
    description =
      "The user's contribution counts, languages and the public projects they contributed to, over their " +
        "whole history, plus one bucket of counts per day or week of the range. By default, the range is the " +
        "365 days ending with `to` (today, unless given), with one bucket per day. " +
        PeriodParams.RANGE_LIMITS + " " +
        "Only contributions to public projects count. An unknown, deleted or disabled user, or one without " +
        "contributions to public projects, returns 404. The counts are approximate: they come from the activity " +
        "log and never go down, so a suggestion made on one day and accepted or declined on a later day counts " +
        "on both. `active` is exact: the number of the user's suggestions that are pending right now, in total " +
        "and per project.",
  )
  fun getProfile(
    @PathVariable userId: Long,
    @ParameterObject period: PeriodParams,
  ): CommunityProfile =
    communityStatsService.getProfile(userId, period) ?: throw NotFoundException(Message.USER_NOT_FOUND)

  @GetMapping("/community-activity")
  @AllowGuestAccess
  @Operation(
    summary = "Get community activity feed",
    description =
      "The user's translation work in public projects, newest first; the sort parameter is ignored. Each entry " +
        "has the shape of an entry in the project activity log, with the project added and the author left out. " +
        "Returns 404 whenever the community profile does.",
  )
  fun getActivityFeed(
    @PathVariable userId: Long,
    @ParameterObject pageable: Pageable,
  ): PagedModel<CommunityActivityModel> {
    rateLimitService.checkPerUserRateLimit(
      "activity",
      limit = tolgeeProperties.rateLimits.activityRequestLimit,
      refillDuration = Duration.ofMillis(tolgeeProperties.rateLimits.activityRequestWindow),
    )
    val page =
      communityStatsService.getActivityFeed(userId, pageable) ?: throw NotFoundException(Message.USER_NOT_FOUND)
    return pagedResourcesAssembler.toModel(page, communityActivityModelAssembler)
  }
}
