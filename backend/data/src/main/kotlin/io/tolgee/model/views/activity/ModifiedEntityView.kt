package io.tolgee.model.views.activity

import io.tolgee.activity.EntityDescription
import io.tolgee.activity.PropertyModification
import io.tolgee.model.activity.ActivityRevision

data class ModifiedEntityView(
  val activityRevision: ActivityRevision,
  val entityClass: String,
  val entityId: Long,
  var modifications: Map<String, PropertyModification> = mutableMapOf(),
  var description: Map<String, Any?>? = null,
  var describingRelations: Map<String, EntityDescription>? = null
)
