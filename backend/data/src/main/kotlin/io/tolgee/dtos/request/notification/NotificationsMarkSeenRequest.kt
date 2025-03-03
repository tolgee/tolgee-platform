package io.tolgee.dtos.request.notification

import io.swagger.v3.oas.annotations.media.Schema

class NotificationsMarkSeenRequest {
  @Schema(example = "[1,2,3]", description = "Notification IDs to be marked as seen")
  var notificationIds: List<Long> = listOf()
}
