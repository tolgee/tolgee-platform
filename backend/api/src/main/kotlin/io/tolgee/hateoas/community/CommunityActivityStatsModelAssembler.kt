package io.tolgee.hateoas.community

import io.tolgee.dtos.community.CommunityActivity
import org.springframework.stereotype.Component

@Component
class CommunityActivityStatsModelAssembler {
  fun toModel(communityActivity: CommunityActivity) =
    CommunityActivityStatsModel(
      buckets = communityActivity.buckets,
      translationsWithPendingSuggestion = communityActivity.translationsWithPendingSuggestion,
      translationsWithAnySuggestion = communityActivity.translationsWithAnySuggestion,
    )
}
