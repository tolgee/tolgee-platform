package io.tolgee.ee.service.connectedApps

import io.tolgee.model.oauth2.OAuth2Grant

data class ConnectedAppProject(
  val id: Long,
  val name: String,
)

data class ConnectedAppView(
  val grant: OAuth2Grant,
  val clientName: String,
  val allProjects: Boolean,
  val projects: List<ConnectedAppProject>,
)
