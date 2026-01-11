package io.tolgee.activity.propChangesProvider

import io.tolgee.activity.data.PropertyModification

class BaseCollectionChangesProvider(
  private val old: Collection<Any?>?,
  private val new: Collection<Any?>?,
  private val extractTargetFromEntity: (Any?) -> Any?,
) {
  fun provide(): PropertyModification? {
    if (old === new) {
      return null
    }

    val oldIds = mapSetToIds(old)
    val newIds = mapSetToIds(new)
    if (oldIds.containsAll(newIds) && newIds.containsAll(oldIds)) {
      return null
    }

    return PropertyModification(oldIds, newIds)
  }

  private fun mapSetToIds(collection: Collection<*>?) = collection?.mapNotNull { extractTargetFromEntity(it) }

  private fun Collection<*>?.containsAll(other: Collection<*>?): Boolean {
    val thisSet = this?.toSet() ?: emptySet()
    val otherSet = other?.toSet() ?: emptySet()
    return thisSet.containsAll(otherSet)
  }
}
