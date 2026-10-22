package io.tolgee.model.views

import java.time.LocalDate

interface CommunityDayRowView {
  val userId: Long
  val languageId: Long
  val day: LocalDate
  val suggested: Int
  val accepted: Int
  val declined: Int
}
