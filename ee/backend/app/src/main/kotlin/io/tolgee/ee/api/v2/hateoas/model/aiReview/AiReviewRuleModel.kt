package io.tolgee.ee.api.v2.hateoas.model.aiReview

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "aiReviewRules", itemRelation = "aiReviewRule")
class AiReviewRuleModel(
  val id: Long,
  val name: String?,
  val enabled: Boolean,
  val tagIn: List<String>,
  val tagNotIn: List<String>,
  val namespaceIn: List<String>,
  val namespaceNotIn: List<String>,
  val languageIds: List<Long>,
  val branchIn: List<String>,
  val branchNotIn: List<String>,
  val threshold: Int,
  val never: Boolean,
  val mode: AiReviewRuleMode,
  val promptId: Long?,
  val skipOpenQaIssues: Boolean,
  val overrides: List<AiReviewRuleOverrideModel>,
  @Schema(description = "Null in the create, update and enable responses. The rule list has the counts.")
  val counts: AiReviewRuleCountsModel?,
) : RepresentationModel<AiReviewRuleModel>()

class AiReviewRuleOverrideModel(
  val languageId: Long,
  val threshold: Int,
  val never: Boolean,
)

class AiReviewRuleCountsModel(
  val autoReviewed: Long,
  val rejected: Long,
  val failed: Long,
  val scored: Long,
  val wouldBeAutoReviewed: Long,
  val backlog: Long,
)
