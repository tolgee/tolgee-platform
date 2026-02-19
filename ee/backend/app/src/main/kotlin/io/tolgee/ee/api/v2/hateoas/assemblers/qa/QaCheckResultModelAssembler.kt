package io.tolgee.ee.api.v2.hateoas.assemblers.qa

import io.tolgee.ee.api.v2.controllers.qa.QaCheckPreviewController
import io.tolgee.ee.api.v2.hateoas.model.qa.QaCheckResultModel
import io.tolgee.ee.service.qa.QaCheckResult
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class QaCheckResultModelAssembler :
  RepresentationModelAssemblerSupport<QaCheckResult, QaCheckResultModel>(
    QaCheckPreviewController::class.java,
    QaCheckResultModel::class.java,
  ) {
  override fun toModel(entity: QaCheckResult): QaCheckResultModel {
    return QaCheckResultModel(
      type = entity.type,
      message = entity.message,
      replacement = entity.replacement,
      positionStart = entity.positionStart,
      positionEnd = entity.positionEnd,
      params = entity.params,
    )
  }
}
