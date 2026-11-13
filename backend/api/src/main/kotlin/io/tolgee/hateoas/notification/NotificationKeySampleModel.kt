package io.tolgee.hateoas.notification

import java.io.Serializable

data class NotificationKeySampleModel(
  val keyId: Long,
  val name: String,
  val namespace: String?,
  val baseText: String?,
) : Serializable
