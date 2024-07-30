package io.tolgee.model.task

import io.tolgee.model.UserAccount
import io.tolgee.model.translation.Translation
import jakarta.persistence.*

@Entity
@IdClass(TaskTranslationId::class)
class TaskTranslation(
  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  var task: Task = Task(),
  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  var translation: Translation = Translation(),
  var done: Boolean = false,
  @ManyToOne(fetch = FetchType.LAZY)
  var author: UserAccount? = null,
)
