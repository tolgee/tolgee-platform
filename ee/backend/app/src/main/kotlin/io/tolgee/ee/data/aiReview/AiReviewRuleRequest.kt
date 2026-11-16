package io.tolgee.ee.data.aiReview

import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class AiReviewRuleRequest(
  @field:NotBlank
  val name: String = "",
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
  val mode: AiReviewRuleMode = AiReviewRuleMode.SCORE_ONLY,
  val promptId: Long? = null,
  val skipOpenQaIssues: Boolean = true,
)

data class AiReviewRuleEnabledRequest(
  val enabled: Boolean,
)
