package io.tolgee.model.views

import java.time.LocalDate
import java.util.Date

interface CommunityProfileRowView {
  val day: LocalDate
  val contributions: Int
  val suggested: Int
  val accepted: Int
  val declined: Int
  val languageTag: String
  val languageName: String
  val projectId: Long
  val projectName: String
  val projectAvatarHash: String?
  val firstContributionAt: Date?
  val lastContributionAt: Date?
}
