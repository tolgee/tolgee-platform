package io.tolgee.fixtures

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

fun dateFromString(
  dateString: String,
  pattern: String = "yyyy-MM-dd",
): Date {
  val formatter = DateTimeFormatter.ofPattern(pattern)
  val localDate = LocalDate.parse(dateString, formatter)
  return Date.from(localDate.atStartOfDay(ZoneId.of("UTC")).toInstant())
}
