package io.tolgee.ee.api.v2.hateoas.assemblers.qa

import io.tolgee.ee.api.v2.controllers.qa.QaCheckPreviewController
import io.tolgee.ee.api.v2.hateoas.model.qa.QaCheckResultModel
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.model.qa.TranslationQaIssue
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class QaCheckResultModelAssembler :
  RepresentationModelAssemblerSupport<QaCheckResult, QaCheckResultModel>(
    QaCheckPreviewController::class.java,
    QaCheckResultModel::class.java,
  ) {
  override fun toModel(entity: QaCheckResult): QaCheckResultModel {
    return toModel(entity, persistedIssues = emptyList())
  }

  fun toModel(
    entity: QaCheckResult,
    persistedIssues: List<TranslationQaIssue>,
  ): QaCheckResultModel {
    val matchingIssue =
      persistedIssues.find { issue ->
        issue.type == entity.type &&
          issue.message == entity.message &&
          issue.positionStart == entity.positionStart &&
          issue.positionEnd == entity.positionEnd
      }
    return QaCheckResultModel(
      type = entity.type,
      message = entity.message,
      replacement = entity.replacement,
      positionStart = entity.positionStart,
      positionEnd = entity.positionEnd,
      params = entity.params,
      ignored = matchingIssue?.state == QaIssueState.IGNORED,
      persistedIssueId = matchingIssue?.id,
    )
  }

  fun toCollectionModel(
    entities: List<QaCheckResult>,
    persistedIssues: List<TranslationQaIssue>,
  ): CollectionModel<QaCheckResultModel> {
    val models = entities.map { toModel(it, persistedIssues) }
    return CollectionModel.of(models)
  }
}
