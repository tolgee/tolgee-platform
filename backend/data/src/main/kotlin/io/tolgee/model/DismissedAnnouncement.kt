package io.tolgee.model

import io.tolgee.model.enums.announcement.Announcement
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.ManyToOne
import java.io.Serializable

@Entity
@IdClass(DismissedAnnouncementId::class)
class DismissedAnnouncement(
  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  var user: UserAccount,
  @Id
  @Enumerated(EnumType.STRING)
  var announcement: Announcement,
) : Serializable
