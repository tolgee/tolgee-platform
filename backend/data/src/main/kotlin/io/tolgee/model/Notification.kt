package io.tolgee.model

import io.tolgee.model.task.Task
import jakarta.persistence.*

@Entity
@Table(
  indexes = [
    Index(columnList = "user_id"),
  ],
)
class Notification : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY)
  lateinit var user: UserAccount

  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project? = null

  @ManyToOne(fetch = FetchType.LAZY)
  var originatingUser: UserAccount? = null

  @ManyToOne(fetch = FetchType.LAZY)
  var linkedTask: Task? = null
}
