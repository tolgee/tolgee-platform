package io.tolgee.model

import io.tolgee.model.enums.Announcement
import java.io.Serializable
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.IdClass
import javax.persistence.ManyToOne

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
