package io.tolgee.hateoas.activity

import io.tolgee.activity.data.ActivityType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "activities", itemRelation = "activity")
class ProjectActivityModel(
  val revisionId: Long,
  val timestamp: Long,
  val type: ActivityType,
  val author: ProjectActivityAuthorModel?,
  val modifiedEntities: Map<String, List<ModifiedEntityModel>>?,
  val meta: Map<String, Any?>?,
  val counts: MutableMap<String, Long>?,
  val params: Any?,
) : RepresentationModel<ProjectActivityModel>(), Serializable
