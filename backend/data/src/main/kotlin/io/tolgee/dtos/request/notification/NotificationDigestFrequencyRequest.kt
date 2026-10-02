package io.tolgee.dtos.request.notification

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.notifications.NotificationDigestFrequency

class NotificationDigestFrequencyRequest(
  @Schema(example = "DAILY")
  var frequency: NotificationDigestFrequency = NotificationDigestFrequency.DAILY,
)
