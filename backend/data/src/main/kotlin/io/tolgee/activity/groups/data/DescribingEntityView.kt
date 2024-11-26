package io.tolgee.activity.groups.data

import io.tolgee.activity.data.EntityDescriptionRef

class DescribingEntityView(
  override val entityId: Long,
  override val entityClass: String,
  val data: Map<String, Any?>,
  val describingRelations: Map<String, EntityDescriptionRef?>,
  override val activityRevisionId: Long,
) : ActivityEntityView {
  inline fun <reified T> getFieldFromViewNullable(name: String): T? {
    return this.data[name] as? T
  }

  inline fun <reified T> getFieldFromView(name: String): T {
    return this.data[name] as? T
      ?: throw IllegalArgumentException("Field $name not found in describing data")
  }
}
