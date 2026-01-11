package io.tolgee.repository

import io.tolgee.model.DismissedAnnouncement
import io.tolgee.model.DismissedAnnouncementId
import io.tolgee.model.enums.announcement.Announcement
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface AnnouncementRepository : JpaRepository<DismissedAnnouncement, DismissedAnnouncementId> {
  @Query(
    """
    select count(*) > 0 from DismissedAnnouncement as an
     where an.user.id = :userId
     and an.announcement = :announcement
  """,
  )
  fun isDismissed(
    userId: Long,
    announcement: Announcement,
  ): Boolean
}
