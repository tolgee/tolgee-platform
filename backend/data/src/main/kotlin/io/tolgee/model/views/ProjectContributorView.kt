package io.tolgee.model.views

import java.util.Date

interface ProjectContributorView {
  val id: Long
  val name: String?
  val avatarHash: String?
  val firstContributionAt: Date
  val lastContributionAt: Date
}
