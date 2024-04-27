package io.tolgee.model.views.activity

import io.tolgee.activity.data.ExistenceEntityDescription
import io.tolgee.activity.data.PropertyModification

data class ModifiedEntityView(
  val entityClass: String,
  val entityId: Long,
  val exists: Boolean?,
  var modifications: Map<String, PropertyModification> = mutableMapOf(),
  var description: Map<String, Any?>? = null,
  var describingRelations: Map<String, ExistenceEntityDescription>? = null,
)
