package io.tolgee.model.views

interface CommunityContributorTotalsView {
  val id: Long
  val name: String?
  val avatarHash: String?
  val suggested: Long
  val accepted: Long
  val declined: Long
}
