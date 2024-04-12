package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.dtos.response.AnnouncementDto
import io.tolgee.model.enums.announcement.Announcement
import io.tolgee.model.enums.announcement.AnnouncementTarget
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.AnnouncementService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/announcement",
  ],
)
@Tag(name = "New features announcements")
class AnnouncementController(
  private val announcementService: AnnouncementService,
  private val authenticationFacade: AuthenticationFacade,
  private val publicBillingConfProvider: PublicBillingConfProvider,
) : IController {
  @GetMapping("")
  @Operation(
    summary = "Get announcement",
    description = "Returns the latest announcement for the currently authenticated user",
  )
  fun getLatest(): AnnouncementDto? {
    val announcement = getLastAnnouncement()
    val user = authenticationFacade.authenticatedUser
    if (this.announcementService.isAnnouncementExpired(announcement)) {
      return null
    }
    if (this.announcementService.isAnnouncementDismissed(announcement, user.id)) {
      return null
    }
    return AnnouncementDto.fromEntity(announcement)
  }

  @PostMapping("dismiss")
  @Operation(
    summary = "Dismiss announcement",
    description = "Dismisses the latest announcement for the currently authenticated user",
  )
  fun dismiss() {
    val announcement = getLastAnnouncement()
    val user = authenticationFacade.authenticatedUser
    announcementService.dismissAnnouncement(announcement, user.id)
  }

  private fun getLastAnnouncement(): Announcement {
    val targets = getAnnouncementTargets()
    return Announcement.entries.last { it.target in targets }
  }

  private fun getAnnouncementTargets() =
    if (publicBillingConfProvider().enabled) {
      listOf(AnnouncementTarget.ALL, AnnouncementTarget.CLOUD)
    } else {
      listOf(AnnouncementTarget.ALL, AnnouncementTarget.SELF_HOSTED)
    }
}
