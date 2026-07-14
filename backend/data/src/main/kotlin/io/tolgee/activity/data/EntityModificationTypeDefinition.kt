package io.tolgee.activity.data

import io.tolgee.model.EntityWithId
import kotlin.reflect.KProperty1

class EntityModificationTypeDefinition<T : EntityWithId>(
  val creation: Boolean = false,
  /**
   * If null then all props can be modified
   */
  val modificationProps: Array<KProperty1<T, *>>? = emptyArray(),
  val deletion: Boolean = false,
)
