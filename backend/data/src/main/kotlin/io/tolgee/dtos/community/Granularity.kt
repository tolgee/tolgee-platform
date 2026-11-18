package io.tolgee.dtos.community

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class Granularity {
  DAY {
    override fun start(day: LocalDate) = day

    override fun next(start: LocalDate) = start.plusDays(1)
  },
  WEEK {
    override fun start(day: LocalDate) = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    override fun next(start: LocalDate) = start.plusWeeks(1)
  },
  ;

  abstract fun start(day: LocalDate): LocalDate

  abstract fun next(start: LocalDate): LocalDate
}
