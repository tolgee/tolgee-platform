package io.tolgee.model.notifications

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(
  indexes = [
    Index(columnList = "user_id"),
  ],
)
class NotificationSetting : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY)
  lateinit var user: UserAccount

  @Enumerated(EnumType.STRING)
  @Column(name = "\"group\"")
  lateinit var group: NotificationTypeGroup

  @Enumerated(EnumType.STRING)
  lateinit var channel: NotificationChannel

  var enabled: Boolean = false
}
