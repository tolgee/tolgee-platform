package io.tolgee.hateoas.notification

import java.io.Serializable

data class NotificationLanguageModel(
  val id: Long,
  val tag: String,
  val name: String,
  val flagEmoji: String?,
) : Serializable
