package io.tolgee.hateoas.activity

import io.tolgee.activity.data.ActivityType
import io.tolgee.api.IProjectActivityModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "activities", itemRelation = "activity")
class ProjectActivityModel(
  override val revisionId: Long,
  override val timestamp: Long,
  override val type: ActivityType,
  override val author: ProjectActivityAuthorModel?,
  override val modifiedEntities: Map<String, List<ModifiedEntityModel>>?,
  override val meta: Map<String, Any?>?,
  override val counts: MutableMap<String, Long>?,
  override val params: Any?,
) : RepresentationModel<ProjectActivityModel>(),
  Serializable,
  IProjectActivityModel
