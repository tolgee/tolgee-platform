package io.tolgee.service

import io.tolgee.component.CurrentDateProvider
import io.tolgee.dtos.response.AnnouncementDto
import io.tolgee.model.DismissedAnnouncement
import io.tolgee.model.enums.Announcement
import io.tolgee.repository.AnnouncementRepository
import io.tolgee.security.AuthenticationFacade
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class AnnouncementService (
  private val announcementRepository: AnnouncementRepository,
  private val authenticationFacade: AuthenticationFacade,
  private val currentDateProvider: CurrentDateProvider
) {
  fun getAnnouncement(): Announcement? {
    val lastAnnouncement = Announcement.values().last()
    val user = this.authenticationFacade.userAccountOrNull

    val dismissedCount = if (user !== null) {
      announcementRepository.getDismissed(user.id, lastAnnouncement)
    } else {
      0L
    }

    val now = currentDateProvider.date.time
    val until = lastAnnouncement.until

    if (dismissedCount == 0L && now < until) {
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
