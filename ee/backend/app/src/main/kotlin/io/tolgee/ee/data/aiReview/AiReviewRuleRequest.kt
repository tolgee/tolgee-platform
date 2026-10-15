package io.tolgee.ee.data.aiReview

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.aiReview.AiReviewLanguageThreshold
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

data class AiReviewRuleRequest(
  @field:Size(max = 255)
  @Schema(description = "Optional rule name")
  val name: String? = null,
  val enabled: Boolean = true,
  @Schema(description = "DRY_RUN only stores the result, ACTIVE marks passing translations as reviewed")
  val mode: AiReviewRuleMode = AiReviewRuleMode.DRY_RUN,
  @field:Min(0)
  @field:Max(100)
  @Schema(description = "Translations scoring this or more are marked as reviewed")
  val threshold: Int = 85,
  @Schema(description = "Per-language threshold overrides. Each entry has either a threshold or never = true.")
  val languageThresholds: List<AiReviewLanguageThreshold> = emptyList(),
  @Schema(description = "Review prompt id. Null uses the built-in default review prompt.")
  val promptId: Long? = null,
  @Schema(description = "Don't auto-review translations with open QA issues")
  val skipWithOpenQaIssues: Boolean = true,
  @Schema(description = "Keys with any of these tags match. Empty string means keys without tags. Empty list = all.")
  val tagIn: List<String> = emptyList(),
  @Schema(description = "Keys with any of these tags don't match. Empty string excludes keys without tags.")
  val tagNotIn: List<String> = emptyList(),
  @Schema(description = "Keys in these namespaces match. Empty string means the default namespace. Empty list = all.")
  val namespaceIn: List<String> = emptyList(),
  @Schema(description = "Keys in these namespaces don't match. Empty string means the default namespace.")
  val namespaceNotIn: List<String> = emptyList(),
  @Schema(description = "Languages the rule applies to. Empty list = all non-base languages.")
  val languageIds: List<Long> = emptyList(),
)
