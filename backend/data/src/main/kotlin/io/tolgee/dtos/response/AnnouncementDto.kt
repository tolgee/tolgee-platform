package io.tolgee.dtos.response

import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Announcement
import java.time.ZoneOffset

class AnnouncementDto (
  val type: Announcement,
) {
  companion object {
    fun fromEntity(announcementEnum: Announcement): AnnouncementDto {
      return AnnouncementDto(type = announcementEnum)
    }
  }
}
