package io.tolgee.ee.data.aiReview

import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class AiReviewRuleRequest(
  val name: String? = null,
  val enabled: Boolean = true,
  val tagIn: List<String> = emptyList(),
  val tagNotIn: List<String> = emptyList(),
  val namespaceIn: List<String> = emptyList(),
  val namespaceNotIn: List<String> = emptyList(),
  val languageIds: List<Long> = emptyList(),
  val branchIn: List<String> = emptyList(),
  val branchNotIn: List<String> = emptyList(),
  @field:Min(0)
  @field:Max(100)
  val threshold: Int = 85,
  val never: Boolean = false,
  val mode: AiReviewRuleMode = AiReviewRuleMode.DRY_RUN,
  val promptId: Long? = null,
  val skipOpenQaIssues: Boolean = true,
  @field:Valid
  val overrides: List<AiReviewRuleOverrideRequest> = emptyList(),
)

data class AiReviewRuleOverrideRequest(
  val languageId: Long,
  @field:Min(0)
  @field:Max(100)
  val threshold: Int = 85,
  val never: Boolean = false,
)

data class AiReviewRuleEnabledRequest(
  val enabled: Boolean,
)
