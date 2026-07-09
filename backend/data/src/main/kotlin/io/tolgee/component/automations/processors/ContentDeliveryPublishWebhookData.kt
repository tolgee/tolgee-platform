package io.tolgee.component.automations.processors

import io.swagger.v3.oas.annotations.media.Schema

data class ContentDeliveryPublishWebhookData(
  @get:Schema(description = "ID of the project the content delivery config belongs to")
  val projectId: Long = 0,
  @get:Schema(description = "ID of the content delivery config that was published")
  val id: Long = 0,
  @get:Schema(description = "Name of the content delivery config")
  val name: String = "",
  @get:Schema(description = "Slug (storage path prefix) of the content delivery config")
  val slug: String = "",
  @get:Schema(description = "Epoch millis when the publish completed")
  val lastPublished: Long? = null,
  @get:Schema(description = "Relative paths of the files that were published")
  val files: List<String> = listOf(),
)
