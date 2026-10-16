package io.tolgee.ee.api.v2.hateoas.model.aiReview

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Keys and languages the rule applies to, used to label rules without a name")
data class AiReviewRuleScopeModel(
  val tagIn: List<String>,
  val tagNotIn: List<String>,
  val namespaceIn: List<String>,
  val namespaceNotIn: List<String>,
  val languageIds: List<Long>,
  val branchIn: List<String>,
  val branchNotIn: List<String>,
)
