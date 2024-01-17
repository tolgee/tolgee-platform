package io.tolgee.model.enums.announcement

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Suppress("unused")
enum class Announcement(
  val until: Long,
  val target: AnnouncementTarget = AnnouncementTarget.ALL,
) {
  FEATURE_BATCH_OPERATIONS(parseTime("2023-09-10 00:00 UTC")),
  FEATURE_MT_FORMALITY(parseTime("2023-10-20 00:00 UTC")),
  FEATURE_CONTENT_DELIVERY_AND_WEBHOOKS(parseTime("2024-01-05 00:00 UTC")),
  NEW_PRICING(parseTime("2024-02-01 00:00 UTC"), AnnouncementTarget.SELF_HOSTED),
  ;

  companion object {
    val last: Announcement
      get() {
        return entries.last()
      }
  }
}

private fun parseTime(datetime: String): Long {
  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")
  return ZonedDateTime.parse(datetime, formatter).toInstant().toEpochMilli()
}
