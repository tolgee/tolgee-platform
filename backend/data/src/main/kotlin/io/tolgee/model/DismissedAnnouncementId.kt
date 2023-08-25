package io.tolgee.model
import io.tolgee.model.enums.Announcement
import java.io.Serializable

class DismissedAnnouncementId : Serializable {
  val user: Long? = null
  val announcement: Announcement? = null
}
