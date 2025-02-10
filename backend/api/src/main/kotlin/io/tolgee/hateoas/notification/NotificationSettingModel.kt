package io.tolgee.hateoas.notification

data class NotificationSettingModel(
  var group: NotificationSettingTypeGroup,
  var enabledForInApp: Boolean,
  var enabledForEmail: Boolean,
)
