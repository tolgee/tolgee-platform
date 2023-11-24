package io.tolgee.util

import java.util.*

fun Date.addMonths(months: Int): Date {
  val calendar = toUtcCalendar()
  calendar.add(Calendar.MONTH, months)
  return calendar.time
}

fun Date.addDays(days: Int): Date {
  val calendar = toUtcCalendar()
  calendar.add(Calendar.DAY_OF_MONTH, days)
  return calendar.time
}

fun Date.addMinutes(minutes: Int): Date {
  val calendar = toUtcCalendar()
  calendar.add(Calendar.MINUTE, minutes)
  return calendar.time
}

fun Date.addSeconds(seconds: Int): Date {
  val calendar = toUtcCalendar()
  calendar.add(Calendar.SECOND, seconds)
  return calendar.time
}

fun Date.addMilliseconds(milliseconds: Int): Date {
  val calendar = toUtcCalendar()
  calendar.add(Calendar.MILLISECOND, milliseconds)
  return calendar.time
}

private fun Date.toUtcCalendar(): Calendar {
  val calendar = getUtcCalendar()
  calendar.time = this
  return calendar
}

private fun getUtcCalendar(): Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
