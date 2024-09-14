package io.tolgee.activity.groups.data

import io.tolgee.activity.data.EntityDescriptionRef
import io.tolgee.activity.data.PropertyModification

class ModifiedEntityView(
  val entityId: Long,
  val entityClass: String,
  val describingData: Map<String, Any?>,
  val describingRelations: Map<String, EntityDescriptionRef?>,
  val modifications: Map<String, PropertyModification>,
  val activityRevisionId: Long,
) {
  inline fun <reified T> getFieldFromViewNullable(name: String): T? {
    return this.describingData[name] as? T ?: this.modifications[name]?.let { it.new as? T }
  }

  inline fun <reified T> getFieldFromView(name: String): T {
    return this.describingData[name] as? T ?: this.modifications[name]?.let { it.new as? T }
      ?: throw IllegalArgumentException("Field $name not found in modifications or describing data")
  }
}
