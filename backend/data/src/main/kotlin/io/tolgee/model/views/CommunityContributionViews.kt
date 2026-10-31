package io.tolgee.model.views

import java.time.LocalDate
import java.util.Date

interface CommunityContributorTotalsView {
  val id: Long
  val name: String?
  val avatarHash: String?
  val contributions: Long
  val suggested: Long
  val accepted: Long
  val declined: Long
  val active: Long
  val firstContributionAt: Date?
  val lastContributionAt: Date?
}

interface CommunityDayRowView {
  val userId: Long
  val languageId: Long
  val day: LocalDate
  val contributions: Int
  val suggested: Int
  val accepted: Int
  val declined: Int
}

interface DayCountsView {
  val day: LocalDate
  val contributions: Long
  val suggested: Long
  val accepted: Long
  val declined: Long
}

interface SuggestedTranslationsView {
  val any: Long
  val pending: Long
}

interface ProjectActiveSuggestionsView {
  val projectId: Long
  val active: Long
}

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
