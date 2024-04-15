package io.tolgee.hateoas.organization.slack

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "workspaces", itemRelation = "workspace")
open class WorkspaceModel(
  val id: Long,
  val slackTeamName: String,
  val slackTeamId: String,
) : RepresentationModel<WorkspaceModel>()
