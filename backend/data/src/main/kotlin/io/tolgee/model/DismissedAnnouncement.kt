package io.tolgee.model

import io.tolgee.model.enums.Announcement
import jakarta.persistence.*
import java.io.Serializable

@Entity
@IdClass(DismissedAnnouncementId::class)
class DismissedAnnouncement(
  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  var user: UserAccount,

  @Id
  @Enumerated(EnumType.STRING)
  var announcement: Announcement
) : Serializable
