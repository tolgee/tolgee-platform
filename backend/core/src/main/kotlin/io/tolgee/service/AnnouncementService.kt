package io.tolgee.service

import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Caches
import io.tolgee.model.DismissedAnnouncement
import io.tolgee.model.enums.announcement.Announcement
import io.tolgee.repository.AnnouncementRepository
import io.tolgee.service.security.UserAccountService
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class AnnouncementService(
  private val announcementRepository: AnnouncementRepository,
  private val currentDateProvider: CurrentDateProvider,
  private val userAccountService: UserAccountService,
) {
  @Cacheable(
    cacheNames = [Caches.DISMISSED_ANNOUNCEMENT],
    key = "{#announcement, #userId}",
  )
  fun isAnnouncementDismissed(
    announcement: Announcement,
    userId: Long,
  ): Boolean {
    return announcementRepository.isDismissed(userId, announcement)
  }

  @CacheEvict(
    cacheNames = [Caches.DISMISSED_ANNOUNCEMENT],
    key = "{#announcement, #userId}",
  )
  fun dismissAnnouncement(
    announcement: Announcement,
    userId: Long,
  ) {
    val user = this.userAccountService.get(userId)
    if (!isAnnouncementDismissed(announcement, user.id)) {
      announcementRepository.save(
        DismissedAnnouncement(announcement = announcement, user = user),
      )
    }
  }

  fun isAnnouncementExpired(announcement: Announcement): Boolean {
    val lastAnnouncement = Announcement.last

    val now = currentDateProvider.date.time
    val until = lastAnnouncement.until

    return now >= until
  }
}
