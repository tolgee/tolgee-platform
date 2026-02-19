package io.tolgee.ee.api.v2.hateoas.assemblers.qa

import io.tolgee.ee.api.v2.controllers.qa.QaIssueController
import io.tolgee.ee.api.v2.hateoas.model.qa.QaIssueModel
import io.tolgee.ee.service.qa.QaIssueService
import io.tolgee.model.qa.TranslationQaIssue
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class QaIssueModelAssembler(
  private val qaIssueService: QaIssueService,
) : RepresentationModelAssemblerSupport<TranslationQaIssue, QaIssueModel>(
    QaIssueController::class.java,
    QaIssueModel::class.java,
  ) {
  override fun toModel(entity: TranslationQaIssue): QaIssueModel {
    return QaIssueModel(
      id = entity.id,
      type = entity.type,
      message = entity.message,
      replacement = entity.replacement,
      positionStart = entity.positionStart,
      positionEnd = entity.positionEnd,
      state = entity.state,
      params = qaIssueService.deserializeParams(entity.params),
    )
  }
}
