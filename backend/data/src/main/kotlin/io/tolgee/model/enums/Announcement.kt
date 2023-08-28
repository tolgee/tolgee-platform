package io.tolgee.model.enums

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Suppress("unused")
enum class Announcement(val until: Long) {
  FEATURE_BATCH_OPERATIONS(parseTime("2023-09-10 00:00 UTC"));

  companion object {
    fun getLast(): Announcement {
      return Announcement.values().last()
    }
  }
}

private fun parseTime(datetime: String): Long {
  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")
  return ZonedDateTime.parse(datetime, formatter).toInstant().toEpochMilli()
}
