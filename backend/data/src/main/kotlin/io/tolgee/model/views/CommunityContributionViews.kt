package io.tolgee.model.views

import java.time.LocalDate

interface CommunityContributorTotalsView {
  val id: Long
  val name: String?
  val avatarHash: String?
  val suggested: Long
  val accepted: Long
  val declined: Long
}

interface CommunityDayRowView {
  val userId: Long
  val languageId: Long
  val day: LocalDate
  val suggested: Int
  val accepted: Int
  val declined: Int
}

interface DayCountsView {
  val day: LocalDate
  val suggested: Long
  val accepted: Long
  val declined: Long
}

interface CommunityProfileRowView {
  val day: LocalDate
  val suggested: Int
  val accepted: Int
  val declined: Int
  val languageTag: String
  val languageName: String
  val projectId: Long
  val projectName: String
  val projectAvatarHash: String?
}
