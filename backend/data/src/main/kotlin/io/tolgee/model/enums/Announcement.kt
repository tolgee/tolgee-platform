package io.tolgee.model.enums

import java.time.LocalDateTime


enum class Announcement(val until: LocalDateTime) {
  FEATURE_BATCH_OPERATIONS(LocalDateTime.of(2023, 9, 1, 0, 0)),
}
