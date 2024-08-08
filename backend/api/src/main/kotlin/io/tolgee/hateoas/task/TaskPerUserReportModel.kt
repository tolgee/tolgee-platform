package io.tolgee.hateoas.task

import io.tolgee.hateoas.userAccount.SimpleUserAccountModel
import org.springframework.hateoas.RepresentationModel

class TaskPerUserReportModel(
  var user: SimpleUserAccountModel,
  var doneItems: Long,
  var baseCharacterCount: Long,
  var baseWordCount: Long,
) : RepresentationModel<TaskPerUserReportModel>()
