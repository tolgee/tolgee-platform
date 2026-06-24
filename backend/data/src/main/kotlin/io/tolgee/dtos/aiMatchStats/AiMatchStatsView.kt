package io.tolgee.dtos.aiMatchStats

/** A requested, non-base language with the display info the per-language rows need. */
data class AiMatchLangInfo(
  val id: Long,
  val tag: String,
  val name: String?,
  val flag: String?,
)

data class AiMatchBucketView(
  val words: Long,
  val keys: Long,
  val langs: Int,
)

data class AiMatchSummaryView(
  val projectId: Long,
  val reviewedAfter: Long?,
  val reviewedBefore: Long?,
  val generatedAt: Long?,
  /** False while a large first-time backfill is still catching up; poll until true. */
  val upToDate: Boolean,
  val b100: AiMatchBucketView,
  val b9990: AiMatchBucketView,
  val b8980: AiMatchBucketView,
  val b7970: AiMatchBucketView,
  val bno: AiMatchBucketView,
  val reviewedWords: Long,
  val reviewedKeys: Long,
  val notReviewedWords: Long,
  val notReviewedKeys: Long,
  val langCount: Int,
  val avgMatchScore: Int,
  val reviewedPct: Int,
)

data class AiMatchLangView(
  val tag: String,
  val name: String?,
  val flag: String?,
  val total: Long,
  val b100: Long,
  val b9990: Long,
  val b8980: Long,
  val b7970: Long,
  val bno: Long,
  val notReviewed: Long,
  val avgMatchScore: Int,
  val b100Pct: Double,
  val b9990Pct: Double,
  val b8980Pct: Double,
  val b7970Pct: Double,
  val bnoPct: Double,
  val notReviewedPct: Double,
)

data class AiMatchLanguagesView(
  val generatedAt: Long?,
  val perLang: List<AiMatchLangView>,
)

data class AiMatchPromptView(
  val promptId: Long?,
  val promptName: String?,
  val provider: String?,
  val versionTs: Long?,
  val versionLabel: String?,
  val total: Long,
  val reviewedWords: Long,
  val reviewedKeys: Long,
  // notReviewed is prompt-level (not split by version); reported on the prompt's newest-version row.
  val notReviewed: Long,
  val notReviewedKeys: Long,
  val b100: Long,
  val b9990: Long,
  val b8980: Long,
  val b7970: Long,
  val bno: Long,
  val avgMatchScore: Int,
  // percentages below are a share of `total` (reviewed + notReviewed), one decimal, like perLang
  val b100Pct: Double,
  val b9990Pct: Double,
  val b8980Pct: Double,
  val b7970Pct: Double,
  val bnoPct: Double,
  val notReviewedPct: Double,
)

data class AiMatchPromptsView(
  val generatedAt: Long?,
  val perPrompt: List<AiMatchPromptView>,
)
