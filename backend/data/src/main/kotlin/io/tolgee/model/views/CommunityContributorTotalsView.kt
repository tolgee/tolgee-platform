package io.tolgee.model.views

import java.util.Date

interface CommunityContributorTotalsView {
  val id: Long
  val name: String?
  val avatarHash: String?
  val contributions: Long
  val suggested: Long
  val accepted: Long
  val declined: Long
  val firstContributionAt: Date?
  val lastContributionAt: Date?
}
