package io.tolgee.model

import io.tolgee.model.enums.Announcement
import java.io.Serializable
import javax.persistence.*

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
