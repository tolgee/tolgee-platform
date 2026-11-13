package io.tolgee.hateoas.community

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.dtos.Avatar
import io.tolgee.dtos.community.LanguageCounts
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.util.Date

@Relation(collectionRelation = "contributors", itemRelation = "contributor")
data class CommunityContributorModel(
  val id: Long,
  val name: String?,
  val avatar: Avatar?,
  @Schema(description = APPROXIMATE)
  val contributions: Long,
  @Schema(description = APPROXIMATE)
  val suggested: Long,
  @Schema(description = APPROXIMATE)
  val accepted: Long,
  @Schema(description = APPROXIMATE)
  val declined: Long,
  @Schema(
    description =
      "Exact: the number of the user's suggestions in this project that are pending right now, whatever the " +
        "range. When `languageIds` is given, only those languages count.",
  )
  val active: Long,
  @Schema(
    description =
      "Whether the user's community profile is visible to others: the user's profile visibility setting, or, " +
        "when left on the default, whether the user has at least one counted contribution in a community " +
        "(public) project, whichever project this list is for. The same for every caller; a user always sees " +
        "their own profile, whatever this says.",
  )
  val profilePublic: Boolean,
  val firstContributionAt: Date?,
  val lastContributionAt: Date?,
  val languages: List<LanguageCounts>,
) : RepresentationModel<CommunityContributorModel>() {
  companion object {
    private const val APPROXIMATE =
      "Approximate: counted from the activity log within the range, and never goes down."
  }
}
