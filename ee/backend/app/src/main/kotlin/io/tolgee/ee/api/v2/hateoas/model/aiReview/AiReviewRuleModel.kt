package io.tolgee.ee.api.v2.hateoas.model.aiReview

import io.tolgee.model.aiReview.AiReviewLanguageThreshold
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "rules", itemRelation = "rule")
data class AiReviewRuleModel(
  val id: Long,
  val name: String?,
  val enabled: Boolean,
  val mode: AiReviewRuleMode,
  val threshold: Int,
  val languageThresholds: List<AiReviewLanguageThreshold>,
  val promptId: Long?,
  val promptName: String?,
  val skipWithOpenQaIssues: Boolean,
  val tagIn: List<String>,
  val tagNotIn: List<String>,
  val namespaceIn: List<String>,
  val namespaceNotIn: List<String>,
  val languageIds: List<Long>,
  val stats: AiReviewRuleStatsModel,
) : RepresentationModel<AiReviewRuleModel>()
