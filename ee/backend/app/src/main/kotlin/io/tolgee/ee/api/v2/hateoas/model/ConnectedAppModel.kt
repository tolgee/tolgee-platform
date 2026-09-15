package io.tolgee.ee.api.v2.hateoas.model

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

data class ConnectedAppProjectModel(
  val id: Long,
  val name: String,
)

@Relation(collectionRelation = "connectedApps", itemRelation = "connectedApp")
open class ConnectedAppModel(
  val id: Long,
  val clientId: String,
  val clientName: String,
  val scopes: List<String>,
  val allProjects: Boolean,
  val projects: List<ConnectedAppProjectModel>,
  val authorizedAt: Long,
  val lastActiveAt: Long?,
) : RepresentationModel<ConnectedAppModel>()
