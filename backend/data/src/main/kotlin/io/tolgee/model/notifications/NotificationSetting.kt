package io.tolgee.model.notifications

import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.task.Task
import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault

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
  lateinit var group: NotificationTypeGroup

  var enabled: Boolean = false
}
