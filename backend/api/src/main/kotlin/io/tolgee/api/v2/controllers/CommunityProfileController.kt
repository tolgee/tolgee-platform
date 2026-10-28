package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.community.CommunityActivityItem
import io.tolgee.dtos.community.CommunityProfile
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
      "Contribution totals, languages, per-day counts for the last 365 days and the public projects " +
        "contributed to. Only contributions to public projects are counted; a user without any is 404.",
  )
  fun getProfile(
    @PathVariable userId: Long,
  ): CommunityProfile = communityStatsService.getProfile(userId) ?: throw NotFoundException(Message.USER_NOT_FOUND)

  @GetMapping("/community-activity")
  @AllowGuestAccess
  @Operation(
    summary = "Get community activity feed",
    description =
      "The user's translation-related activity in public projects, always newest first (the sort parameter " +
        "is ignored), in the shape of the project activity log plus the project and without the author. " +
        "Visible under the same rule as the community profile.",
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
