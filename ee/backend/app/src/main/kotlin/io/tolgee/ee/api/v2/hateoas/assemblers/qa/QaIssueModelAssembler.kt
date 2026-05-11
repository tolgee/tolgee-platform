package io.tolgee.ee.api.v2.hateoas.assemblers.qa

import io.tolgee.hateoas.qa.QaIssueModel
import io.tolgee.model.qa.TranslationQaIssue
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component
import io.tolgee.hateoas.qa.QaIssueModelAssembler as CoreQaIssueModelAssembler

@Component("eeQaIssueModelAssembler")
class QaIssueModelAssembler(
  private val coreAssembler: CoreQaIssueModelAssembler,
) : RepresentationModelAssembler<TranslationQaIssue, QaIssueModel> {
  override fun toModel(entity: TranslationQaIssue): QaIssueModel {
    return coreAssembler.toModel(entity)
  }

  override fun toCollectionModel(entities: Iterable<TranslationQaIssue>): CollectionModel<QaIssueModel> {
    return CollectionModel.of(entities.map { toModel(it) })
  }
}
