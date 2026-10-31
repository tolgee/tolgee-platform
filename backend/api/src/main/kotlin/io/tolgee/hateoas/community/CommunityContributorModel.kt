package io.tolgee.hateoas.community

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.dtos.Avatar
import io.tolgee.dtos.community.Bucket
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
  @get:JsonFormat(shape = JsonFormat.Shape.STRING)
  val firstContributionAt: Date?,
  @get:JsonFormat(shape = JsonFormat.Shape.STRING)
  val lastContributionAt: Date?,
  val languages: List<LanguageCounts>,
  val buckets: List<Bucket>,
) : RepresentationModel<CommunityContributorModel>() {
  companion object {
    private const val APPROXIMATE =
      "Approximate: counted from the activity log within the range, and never goes down."
  }
}
