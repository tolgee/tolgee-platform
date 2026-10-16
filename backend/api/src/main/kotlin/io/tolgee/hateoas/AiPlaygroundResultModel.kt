package io.tolgee.hateoas

import io.tolgee.hateoas.aiReview.AiReviewIssueModel
import io.tolgee.model.enums.PromptType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "results", itemRelation = "result")
class AiPlaygroundResultModel(
  val keyId: Long,
  val languageId: Long,
  val translation: String?,
  val contextDescription: String?,
  val type: PromptType = PromptType.TRANSLATE,
  val reviewScore: Int? = null,
  val reviewIssues: List<AiReviewIssueModel>? = null,
  val reviewError: String? = null,
) : RepresentationModel<AiPlaygroundResultModel>()
