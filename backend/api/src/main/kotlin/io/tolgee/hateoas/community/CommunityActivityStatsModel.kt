package io.tolgee.hateoas.community

import io.tolgee.dtos.community.Bucket

@Suppress("unused")
open class CommunityActivityStatsModel(
  val buckets: List<Bucket>,
  val translationsWithPendingSuggestion: Long,
  val translationsWithAnySuggestion: Long,
)
