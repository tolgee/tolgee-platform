package io.tolgee.activity.groups.data

import io.tolgee.activity.data.PropertyModification

class ModifiedEntityView(
  val entityId: Long,
  val entityClass: String,
  val describingData: Map<String, Any?>,
  val describingRelations: Map<String, Any?>,
  val modifications: Map<String, PropertyModification>,
) {
  private inline fun <reified T> Map<String, Any?>.getFieldFromMap(name: String): T? {
    this[name].let {
      return it as? T
    }
  }

  inline fun <reified T> getFieldFromViewNullable(name: String): T? {
    return this.describingData[name] as? T ?: this.modifications[name]?.let { it.new as? T }
  }

  inline fun <reified T> getFieldFromView(name: String): T {
    return this.describingData[name] as? T ?: this.modifications[name]?.let { it.new as? T }
      ?: throw IllegalArgumentException("Field $name not found in modifications or describing data")
  }
}
