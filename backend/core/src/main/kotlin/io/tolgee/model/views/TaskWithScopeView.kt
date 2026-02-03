package io.tolgee.model.views

import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.TaskState
import io.tolgee.model.enums.TaskType
import io.tolgee.model.task.TaskKey
import io.tolgee.model.translationAgency.TranslationAgency
import java.util.Date

data class TaskWithScopeView(
  val project: Project,
  val number: Long,
  val name: String?,
  val description: String,
  val type: TaskType,
  val language: Language,
  val dueDate: Date?,
  val assignees: MutableSet<UserAccount>,
  val keys: MutableSet<TaskKey>,
  val author: UserAccount,
  val createdAt: Date?,
  val state: TaskState,
  val closedAt: Date?,
  val totalItems: Long,
  val doneItems: Long,
  val baseWordCount: Long,
  val baseCharacterCount: Long,
  val agency: TranslationAgency?,
)
