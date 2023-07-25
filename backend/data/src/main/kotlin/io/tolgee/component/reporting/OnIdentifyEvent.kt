package io.tolgee.component.reporting

data class OnIdentifyEvent(
  val userAccountId: Long,
  val anonymousUserId: String
)
