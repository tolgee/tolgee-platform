package io.tolgee.activity.groups.viewProviders.generic

import io.tolgee.activity.data.EntityDescriptionRef
import io.tolgee.activity.data.PropertyModification

data class GenericGroupItemModel(
  val entityClass: String,
  val entityId: Long,
  val description: Map<String, Any?>,
  val describingRelations: Map<String, EntityDescriptionRef?>,
  val modifications: Map<String, PropertyModification>,
)
