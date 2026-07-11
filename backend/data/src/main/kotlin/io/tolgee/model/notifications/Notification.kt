package io.tolgee.model.notifications

import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.task.Task
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnDefault

@Entity
@Table(
  indexes = [
    Index(columnList = "user_id"),
  ],
)
class Notification : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY)
  lateinit var user: UserAccount

  @Enumerated(EnumType.STRING)
  @ColumnDefault("TASK_ASSIGNED")
  var type: NotificationType = NotificationType.TASK_ASSIGNED

  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project? = null

  @ManyToOne(fetch = FetchType.LAZY)
  var originatingUser: UserAccount? = null

  @ManyToOne(fetch = FetchType.LAZY)
  var linkedTask: Task? = null

  @ColumnDefault("false")
  var seen: Boolean = false
}
