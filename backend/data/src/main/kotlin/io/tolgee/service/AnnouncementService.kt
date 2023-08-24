package io.tolgee.service

import io.tolgee.model.DismissedAnnouncement
import io.tolgee.model.enums.Announcement
import io.tolgee.repository.AnnouncementRepository
import io.tolgee.security.AuthenticationFacade
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AnnouncementService (
  private val announcementRepository: AnnouncementRepository,
  private val authenticationFacade: AuthenticationFacade,
) {
  fun getAnnouncement(): Announcement? {
    val lastAnnouncement = Announcement.values().last()
    val user = this.authenticationFacade.userAccountOrNull

    val dismissedCount = if (user !== null) {
      announcementRepository.getDismissed(user.id, lastAnnouncement)
    } else {
      0L
    }

    if (dismissedCount == 0L && lastAnnouncement.until.isAfter(LocalDateTime.now()) ) {
      return lastAnnouncement
    }
    return null
  }

  fun dismissAnnouncement() {
    val announcement = getAnnouncement()
    val user = this.authenticationFacade.userAccountEntity

    if (announcement !== null) {
      announcementRepository.save(
        DismissedAnnouncement(announcement = announcement, user = user)
      )
    }
  }
}
