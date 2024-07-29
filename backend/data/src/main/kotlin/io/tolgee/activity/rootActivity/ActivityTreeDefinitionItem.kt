package io.tolgee.activity.rootActivity

import io.tolgee.model.EntityWithId
import kotlin.reflect.KClass

/**
 * In some cases we need to return the data in activities in structured way by some root entity, for example for import
 * we need to return the data rooted by key, so we can list the data on front-end nicely.
 */
class ActivityTreeDefinitionItem(
  val entityClass: KClass<out EntityWithId>,
  /**
   * The field of the child entity that describes the parent entity.
   * E.g., For `Translation` entity this would be the `key`
   */
  val describingField: String? = null,
  val single: Boolean = false,
  val children: Map<String, ActivityTreeDefinitionItem> = emptyMap(),
)
