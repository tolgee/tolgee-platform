package io.tolgee.model.task

import io.tolgee.model.UserAccount
import io.tolgee.model.key.Key
import jakarta.persistence.*

@Entity
@IdClass(TaskKeyId::class)
class TaskKey(
  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  var task: Task = Task(),
  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  var key: Key = Key(),
  var done: Boolean = false,
  @ManyToOne(fetch = FetchType.LAZY)
  var author: UserAccount? = null,
)
