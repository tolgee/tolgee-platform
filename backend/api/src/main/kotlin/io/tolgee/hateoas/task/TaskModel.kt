package io.tolgee.hateoas.task

import io.tolgee.hateoas.language.LanguageModel
import io.tolgee.hateoas.userAccount.UserAccountModel
import io.tolgee.model.enums.TaskState
import io.tolgee.model.enums.TaskType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "tasks", itemRelation = "task")
class TaskModel(
  var id: Long = 0L,
  var name: String = "",
  var description: String = "",
  var type: TaskType = TaskType.TRANSLATE,
  var language: LanguageModel,
  var dueDate: Long? = null,
  var assignees: MutableSet<UserAccountModel> = mutableSetOf(),
  var totalItems: Long = 0,
  var doneItems: Long = 0,
  var baseWordCount: Long = 0,
  var author: UserAccountModel? = null,
  var createdAt: Long = 0,
  var closedAt: Long? = null,
  var state: TaskState = TaskState.IN_PROGRESS,
) : RepresentationModel<TaskModel>()
