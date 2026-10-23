package io.tolgee.hateoas.community

import io.tolgee.dtos.Avatar
import io.tolgee.dtos.community.LanguageCounts
import io.tolgee.dtos.community.WeekBucket
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.util.Date

@Relation(collectionRelation = "contributors", itemRelation = "contributor")
data class CommunityContributorModel(
  val id: Long,
  val name: String?,
  val avatar: Avatar?,
  val contributions: Long,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
  val firstContributionAt: Date?,
  val lastContributionAt: Date?,
  val languages: List<LanguageCounts>,
  val weeks: List<WeekBucket>,
) : RepresentationModel<CommunityContributorModel>()
