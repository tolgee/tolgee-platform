@file:Suppress("PropertyName")

package io.tolgee.ee.service.slackIntegration

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class ConnectToSlackResponse(
  val access_token: String?,
  val team: TeamResponse?,
  val error: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
class TeamResponse(
  val name: String,
  val id: String,
)
