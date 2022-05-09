package io.tolgee.model.views.activity

import io.tolgee.activity.data.ExistenceEntityDescription
import io.tolgee.activity.data.PropertyModification
import io.tolgee.model.activity.ActivityRevision

data class ModifiedEntityView(
  val activityRevision: ActivityRevision,
  val entityClass: String,
  val entityId: Long,
  val exists: Boolean?,
  var modifications: Map<String, PropertyModification> = mutableMapOf(),
  var description: Map<String, Any?>? = null,
  var describingRelations: Map<String, ExistenceEntityDescription>? = null,
)
