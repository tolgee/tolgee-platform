package io.tolgee.api.v2.hateoas.activity

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "activities", itemRelation = "activity")
class ProjectActivityModel(
  val revisionId: Long,
  val timestamp: Long,
  val type: String,
  val author: ProjectActivityAuthorModel?,
  val modifiedEntities: Map<String, List<ModifiedEntityModel>>?,
  val meta: Map<String, Any?>?,
) : RepresentationModel<ProjectActivityModel>(), Serializable
