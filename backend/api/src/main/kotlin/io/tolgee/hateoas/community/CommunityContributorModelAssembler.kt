package io.tolgee.hateoas.community

import io.tolgee.api.v2.controllers.project.CommunityStatsController
import io.tolgee.dtos.community.CommunityContributorStats
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class CommunityContributorModelAssembler :
  RepresentationModelAssemblerSupport<CommunityContributorStats, CommunityContributorModel>(
    CommunityStatsController::class.java,
    CommunityContributorModel::class.java,
  ) {
  override fun toModel(stats: CommunityContributorStats): CommunityContributorModel {
    return CommunityContributorModel(
      id = stats.id,
      name = stats.name,
      avatar = stats.avatar,
      contributions = stats.contributions,
      suggested = stats.suggested,
      accepted = stats.accepted,
      declined = stats.declined,
      active = stats.active,
      firstContributionAt = stats.firstContributionAt,
      lastContributionAt = stats.lastContributionAt,
      languages = stats.languages,
      buckets = stats.buckets,
    )
  }
}
