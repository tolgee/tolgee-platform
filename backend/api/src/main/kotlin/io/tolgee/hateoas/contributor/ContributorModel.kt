package io.tolgee.hateoas.contributor

import io.tolgee.dtos.Avatar
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.util.Date

@Relation(collectionRelation = "contributors", itemRelation = "contributor")
data class ContributorModel(
  val id: Long,
  val username: String,
  val name: String?,
  val avatar: Avatar?,
  val firstContributionAt: Date,
  val lastContributionAt: Date,
  val invitationPending: Boolean,
) : RepresentationModel<ContributorModel>()
