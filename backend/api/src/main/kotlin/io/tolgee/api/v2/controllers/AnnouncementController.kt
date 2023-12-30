package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.response.AnnouncementDto
import io.tolgee.model.enums.Announcement
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
) : IController {
  @GetMapping("")
  @Operation(description = "Get latest announcement")
  fun getLatest(): AnnouncementDto? {
    val announcement = Announcement.last
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
  @Operation(description = "Dismiss current announcement for current user")
  fun dismiss() {
    val announcement = Announcement.last
    val user = authenticationFacade.authenticatedUser
    announcementService.dismissAnnouncement(announcement, user.id)
  }
}
