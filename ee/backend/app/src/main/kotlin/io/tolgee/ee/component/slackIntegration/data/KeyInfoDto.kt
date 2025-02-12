package io.tolgee.ee.component.slackIntegration.data

data class KeyInfoDto(
  val id: Long,
  val name: String,
  val tags: Set<String>?,
  val namespace: String?,
  val description: String?,
)
