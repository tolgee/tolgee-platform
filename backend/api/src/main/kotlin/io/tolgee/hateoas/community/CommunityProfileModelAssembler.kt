package io.tolgee.hateoas.community

import io.tolgee.dtos.community.CommunityProfile
import org.springframework.stereotype.Component

@Component
class CommunityProfileModelAssembler {
  fun toModel(communityProfile: CommunityProfile) =
    CommunityProfileModel(
      id = communityProfile.id,
      name = communityProfile.name,
      avatar = communityProfile.avatar,
      contributions = communityProfile.contributions,
      suggested = communityProfile.suggested,
      accepted = communityProfile.accepted,
      declined = communityProfile.declined,
      languages = communityProfile.languages,
      projects = communityProfile.projects,
      days = communityProfile.days,
    )
}
