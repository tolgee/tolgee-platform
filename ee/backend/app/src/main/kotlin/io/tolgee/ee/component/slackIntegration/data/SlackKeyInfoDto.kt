package io.tolgee.ee.component.slackIntegration.data

/**
 * Slack specific view of [io.tolgee.model.key.Key]
 */
data class SlackKeyInfoDto(
  val id: Long,
  val name: String,
  val tags: Set<String>?,
  val namespace: String?,
  val description: String?,
)
