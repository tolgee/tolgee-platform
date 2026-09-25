package io.tolgee.component.automations.processors

data class ContentDeliveryPublishWebhookData(
  val id: Long,
  val name: String,
  val slug: String,
  val lastPublished: Long?,
  val files: List<String>,
)
