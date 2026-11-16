package io.tolgee.hateoas

import io.tolgee.model.aiReview.AiReviewIssue
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "results", itemRelation = "result")
class AiPlaygroundResultModel(
  val keyId: Long,
  val languageId: Long,
  val translation: String?,
  val contextDescription: String?,
  val score: Int? = null,
  val issues: List<AiReviewIssue>? = null,
) : RepresentationModel<AiPlaygroundResultModel>()
