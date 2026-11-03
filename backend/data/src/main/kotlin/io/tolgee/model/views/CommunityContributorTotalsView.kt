package io.tolgee.model.views

import io.tolgee.model.enums.ProfileVisibility
import java.util.Date

interface CommunityContributorTotalsView {
  val id: Long
  val name: String?
  val avatarHash: String?
  val profileVisibility: ProfileVisibility
  val contributions: Long
  val suggested: Long
  val accepted: Long
  val declined: Long
  val active: Long
  val firstContributionAt: Date?
  val lastContributionAt: Date?
}
