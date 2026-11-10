package io.tolgee.hateoas.community

import io.tolgee.activity.data.ActivityType
import io.tolgee.hateoas.activity.ModifiedEntityModel
import io.tolgee.hateoas.project.SimpleProjectModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "activities", itemRelation = "activity")
class CommunityActivityModel(
  val revisionId: Long,
  val timestamp: Long,
  val type: ActivityType,
  val project: SimpleProjectModel,
  val modifiedEntities: Map<String, List<ModifiedEntityModel>>?,
  val meta: Map<String, Any?>?,
  val counts: MutableMap<String, Long>?,
  val params: Any?,
) : RepresentationModel<CommunityActivityModel>()
