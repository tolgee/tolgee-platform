package io.tolgee.repository

import io.reactivex.rxjava3.core.Maybe
import io.tolgee.model.DismissedAnnouncement
import io.tolgee.model.enums.Announcement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AnnouncementRepository : JpaRepository<DismissedAnnouncement, Long> {
  @Query("""
    select count(*) from DismissedAnnouncement as an
     where an.user.id = :userId
     and an.announcement = :announcement
  """)
  fun getDismissed(userId: Long, announcement: Announcement): Long
}
