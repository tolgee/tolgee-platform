package io.tolgee.dtos.community

data class CommunityActivity(
  val buckets: List<Bucket>,
  val translationsWithPendingSuggestion: Long,
  val translationsWithAnySuggestion: Long,
)
