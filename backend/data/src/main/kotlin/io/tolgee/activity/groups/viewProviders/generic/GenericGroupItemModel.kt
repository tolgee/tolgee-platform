package io.tolgee.activity.groups.viewProviders.generic

import io.tolgee.activity.data.PropertyModification

data class GenericGroupItemModel(
  val entityClass: String,
  val entityId: Long,
  val description: Map<String, Any?>,
  /**
   * Related entities describing this one (e.g. the key and language of a translation),
   * keyed by the relation name, with their describing data resolved.
   */
  val relations: Map<String, GenericGroupItemRelationModel>,
  val modifications: Map<String, PropertyModification>,
)

data class GenericGroupItemRelationModel(
  val entityClass: String,
  val entityId: Long,
  val data: Map<String, Any?>,
)
