package io.tolgee.dtos.request.notification

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.model.notifications.NotificationTypeGroup

class NotificationSettingsRequest(
  @Schema(example = "TASKS")
  var group: NotificationTypeGroup,
  @Schema(example = "IN_APP")
  var channel: NotificationChannel,
  @Schema(example = "false", description = "True if the setting should be enabled, false for disabled")
  var enabled: Boolean,
)
