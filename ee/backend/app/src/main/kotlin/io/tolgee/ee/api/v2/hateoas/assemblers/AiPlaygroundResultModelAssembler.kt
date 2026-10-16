package io.tolgee.ee.api.v2.hateoas.assemblers

import io.tolgee.ee.api.v2.controllers.AiPlaygroundResultController
import io.tolgee.hateoas.AiPlaygroundResultModel
import io.tolgee.hateoas.aiReview.AiReviewIssueModel
import io.tolgee.model.AiPlaygroundResult
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class AiPlaygroundResultModelAssembler :
  RepresentationModelAssemblerSupport<AiPlaygroundResult, AiPlaygroundResultModel>(
    AiPlaygroundResultController::class.java,
    AiPlaygroundResultModel::class.java,
  ) {
  override fun toModel(entity: AiPlaygroundResult): AiPlaygroundResultModel {
    return AiPlaygroundResultModel(
      keyId = entity.key!!.id,
      languageId = entity.language!!.id,
      translation = entity.translation,
      contextDescription = entity.contextDescription,
      type = entity.type,
      reviewScore = entity.reviewScore,
      reviewIssues = entity.reviewIssues?.map { AiReviewIssueModel.of(it) },
      reviewError = entity.reviewError,
    )
  }
}
