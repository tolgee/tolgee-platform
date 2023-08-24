package io.tolgee.model.enums

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun parseTime(datetime: String): Long {
  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")
  return ZonedDateTime.parse(datetime, formatter).toInstant().toEpochMilli()
}

enum class Announcement(val until: Long) {
  FEATURE_BATCH_OPERATIONS(parseTime("2023-09-10 00:00 UTC")),
}
