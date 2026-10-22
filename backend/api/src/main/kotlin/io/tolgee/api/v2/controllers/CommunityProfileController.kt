package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Message
import io.tolgee.dtos.community.CommunityProfile
import io.tolgee.exceptions.NotFoundException
import io.tolgee.service.contributor.CommunityStatsService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/users/{userId:[0-9]+}"])
@Tag(
  name = "Community profile",
  description = "A signed-in user's view of another user's contributions to public projects",
)
class CommunityProfileController(
  private val communityStatsService: CommunityStatsService,
) {
  @GetMapping("/community-profile")
  @Operation(
    summary = "Get community profile",
    description =
      "Contribution totals, languages, per-day counts for the last 365 days, recent contributions and " +
        "the public projects contributed to. Only contributions to public projects are counted; " +
        "a user without any is 404.",
  )
  fun getProfile(
    @PathVariable userId: Long,
  ): CommunityProfile = communityStatsService.getProfile(userId) ?: throw NotFoundException(Message.USER_NOT_FOUND)
}
