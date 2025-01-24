package io.tolgee.model

import io.tolgee.constants.NotificationType
import io.tolgee.model.task.Task
import jakarta.persistence.*
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
