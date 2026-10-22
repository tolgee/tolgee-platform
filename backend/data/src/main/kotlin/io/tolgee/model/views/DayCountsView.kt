package io.tolgee.model.views

import java.time.LocalDate

interface DayCountsView {
  val day: LocalDate
  val suggested: Long
  val accepted: Long
  val declined: Long
}
