package io.tolgee.model.notifications

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnDefault
import java.util.Date

@Entity
@Table(name = "notification_digest_state")
class NotificationDigestState(
  @Id
  @Column(name = "user_id")
  var userId: Long = 0,
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @ColumnDefault("DAILY")
  var frequency: NotificationDigestFrequency = NotificationDigestFrequency.DAILY,
  var lastDigestSentAt: Date? = null,
)
