package io.tolgee.activity.groups.data

import io.tolgee.activity.data.EntityDescriptionRef
import io.tolgee.activity.data.PropertyModification

class ModifiedEntityView(
  override val entityId: Long,
  override val entityClass: String,
  val describingData: Map<String, Any?>,
  val describingRelations: Map<String, EntityDescriptionRef?>,
  val modifications: Map<String, PropertyModification>,
  override val activityRevisionId: Long,
  override val additionalDescription: Map<String, Any?>? = null,
) : ActivityEntityView
