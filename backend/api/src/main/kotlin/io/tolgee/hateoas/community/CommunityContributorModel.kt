package io.tolgee.hateoas.community

import io.tolgee.dtos.Avatar
import io.tolgee.dtos.community.LanguageCounts
import io.tolgee.dtos.community.WeekBucket
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "contributors", itemRelation = "contributor")
data class CommunityContributorModel(
  val id: Long,
  val name: String?,
  val avatar: Avatar?,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
  val languages: List<LanguageCounts>,
  val weeks: List<WeekBucket>,
) : RepresentationModel<CommunityContributorModel>()
