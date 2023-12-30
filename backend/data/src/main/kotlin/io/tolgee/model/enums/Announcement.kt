package io.tolgee.model.enums

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Suppress("unused")
enum class Announcement(val until: Long) {
  FEATURE_BATCH_OPERATIONS(parseTime("2023-09-10 00:00 UTC")),
  FEATURE_MT_FORMALITY(parseTime("2023-10-20 00:00 UTC")),
  FEATURE_CONTENT_DELIVERY_AND_WEBHOOKS(parseTime("2024-01-05 00:00 UTC")),
  ;

  companion object {
    val last: Announcement
      get() {
        return Announcement.values().last()
      }
  }
}

private fun parseTime(datetime: String): Long {
  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")
  return ZonedDateTime.parse(datetime, formatter).toInstant().toEpochMilli()
}
