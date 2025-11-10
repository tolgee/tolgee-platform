package io.tolgee.util

import org.apache.commons.lang3.ObjectUtils.max
import org.apache.commons.lang3.ObjectUtils.min
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

fun Date.addMonths(months: Int): Date = addToCalendar(Calendar.MONTH, months)

fun Date.addDays(days: Int): Date = addToCalendar(Calendar.DAY_OF_MONTH, days)

fun Date.addMinutes(minutes: Int): Date = addToCalendar(Calendar.MINUTE, minutes)

fun Date.addSeconds(seconds: Int): Date = addToCalendar(Calendar.SECOND, seconds)

fun Date.addMilliseconds(milliseconds: Int): Date = addToCalendar(Calendar.MILLISECOND, milliseconds)

private fun Date.addToCalendar(
  field: Int,
  amount: Int,
): Date {
  val calendar = toUtcCalendar()
  calendar.add(field, amount)
  return calendar.time
}

private fun Date.toUtcCalendar(): Calendar {
  val calendar = getUtcCalendar()
  calendar.time = this
  return calendar
}

fun Date.fullMonthsDiff(date: Date): Int {
  val first = min(this, date)
  val second = max(this, date)

  var fullMonths = 0
  while (first.addMonths(fullMonths) < second) {
    fullMonths++
  }

  return fullMonths
}

private fun getUtcCalendar(): Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
