package io.tolgee.ee.api.v2.hateoas.assemblers.qa

import io.tolgee.ee.api.v2.controllers.qa.QaIssueController
import io.tolgee.hateoas.qa.QaIssueModel
import io.tolgee.model.qa.TranslationQaIssue
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component
import io.tolgee.hateoas.qa.QaIssueModelAssembler as CoreQaIssueModelAssembler

@Component("eeQaIssueModelAssembler")
class QaIssueModelAssembler(
  private val coreAssembler: CoreQaIssueModelAssembler,
) : RepresentationModelAssemblerSupport<TranslationQaIssue, QaIssueModel>(
    QaIssueController::class.java,
    QaIssueModel::class.java,
  ) {
  override fun toModel(entity: TranslationQaIssue): QaIssueModel {
    return coreAssembler.toModel(entity)
  }
}
