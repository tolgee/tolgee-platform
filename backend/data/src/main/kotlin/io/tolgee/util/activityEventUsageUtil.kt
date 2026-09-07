package io.tolgee.util

import io.tolgee.activity.data.RevisionType
import io.tolgee.events.OnProjectActivityEvent
import kotlin.reflect.KClass

/**
 * Creation lists every property it sets among its modifications, so without the revision type a new
 * key reads as both a deletion and a rename.
 */
fun OnProjectActivityEvent.hasChangeTo(
  type: KClass<*>,
  vararg properties: String,
): Boolean =
  modifiedEntities[type]?.any { entity ->
    entity.value.revisionType == RevisionType.MOD &&
      properties.any { entity.value.modifications.contains(it) }
  } == true

/** Both directions: a restore changes what is counted as much as a deletion does. */
fun OnProjectActivityEvent.hasDeletionStateChangeOf(vararg types: KClass<*>): Boolean =
  types.any { hasChangeTo(it, "deletedAt") }
