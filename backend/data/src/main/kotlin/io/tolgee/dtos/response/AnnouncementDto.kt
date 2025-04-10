package io.tolgee.dtos.response

import io.tolgee.model.enums.announcement.Announcement

class AnnouncementDto(
  val type: Announcement,
) {
  companion object {
    fun fromEntity(announcementEnum: Announcement): AnnouncementDto = AnnouncementDto(type = announcementEnum)
  }
}
