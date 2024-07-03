package io.tolgee.hateoas.task

import io.tolgee.api.v2.controllers.TaskController
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.hateoas.language.LanguageModelAssembler
import io.tolgee.hateoas.project.SimpleProjectModelAssembler
import io.tolgee.hateoas.userAccount.UserAccountModelAssembler
import io.tolgee.model.task.Task
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TaskWithProjectModelAssembler(
  private val userAccountModelAssembler: UserAccountModelAssembler,
  private val languageModelAssembler: LanguageModelAssembler,
  private val simpleProjectModelAssembler: SimpleProjectModelAssembler,
) : RepresentationModelAssemblerSupport<Task, TaskWithProjectModel>(
    TaskController::class.java,
    TaskWithProjectModel::class.java,
  ) {
  override fun toModel(entity: Task): TaskWithProjectModel {
    return TaskWithProjectModel(
      id = entity.id,
      name = entity.name,
      description = entity.description,
      type = entity.type,
      language =
        entity.language.let {
          languageModelAssembler.toModel(
            LanguageDto.fromEntity(
              it,
              entity.project.baseLanguage?.id,
            ),
          )
        },
      dueDate = entity.dueDate?.time,
      assignees = entity.assignees.map { userAccountModelAssembler.toModel(it) }.toMutableSet(),
      author = entity.author?.let { userAccountModelAssembler.toModel(it) },
      createdAt = entity.createdAt.time,
      closedAt = entity.closedAt?.time,
      totalItems = entity.getTotalItems(),
      doneItems = entity.getDoneItems(),
      baseWordCount = entity.getBaseWordCount(),
      state = entity.state,
      project = entity.project.let { simpleProjectModelAssembler.toModel(it) },
    )
  }
}
