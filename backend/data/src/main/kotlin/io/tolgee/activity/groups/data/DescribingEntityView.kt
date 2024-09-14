package io.tolgee.activity.groups.data

import io.tolgee.activity.data.EntityDescriptionRef

class DescribingEntityView(
  val entityId: Long,
  val entityClass: String,
  val data: Map<String, Any?>,
  val describingRelations: Map<String, EntityDescriptionRef?>,
  val activityRevisionId: Long,
)
