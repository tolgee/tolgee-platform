package io.tolgee.activity.groups.data

import io.tolgee.activity.data.EntityDescriptionRef
import io.tolgee.activity.data.PropertyModification

class ModifiedEntityView(
  val entityId: Long,
  val entityClass: String,
  val describingData: Map<String, Any?>,
  val describingRelations: Map<String, EntityDescriptionRef?>,
  val modifications: Map<String, PropertyModification<Any?>>,
  val activityRevisionId: Long,
) : ActivityEntityView
