package io.tolgee.model

import io.tolgee.model.task.Task
import jakarta.persistence.*
import java.util.*

@Entity
@Table(
  indexes = [
    Index(columnList = "user_id"),
  ],
)
class Notification(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long = 0L,

  @ManyToOne(fetch = FetchType.LAZY)
  var user: UserAccount,

  var createdAt: Date = Date(),

  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  var originatingUser: UserAccount? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  var linkedTask: Task? = null,
)
