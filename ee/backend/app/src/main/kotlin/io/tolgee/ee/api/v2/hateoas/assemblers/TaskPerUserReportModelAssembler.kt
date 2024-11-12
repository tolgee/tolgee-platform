package io.tolgee.ee.api.v2.hateoas.assemblers

import io.tolgee.ee.api.v2.controllers.TaskController
import io.tolgee.hateoas.task.TaskPerUserReportModel
import io.tolgee.hateoas.userAccount.SimpleUserAccountModelAssembler
import io.tolgee.model.views.TaskPerUserReportView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TaskPerUserReportModelAssembler(
  private val simpleUserAccountModelAssembler: SimpleUserAccountModelAssembler,
) : RepresentationModelAssemblerSupport<TaskPerUserReportView, TaskPerUserReportModel>(
    TaskController::class.java,
    TaskPerUserReportModel::class.java,
  ) {
  override fun toModel(entity: TaskPerUserReportView): TaskPerUserReportModel {
    return TaskPerUserReportModel(
      user = simpleUserAccountModelAssembler.toModel(entity.user),
      doneItems = entity.doneItems,
      baseCharacterCount = entity.baseCharacterCount,
      baseWordCount = entity.baseWordCount,
    )
  }
}
