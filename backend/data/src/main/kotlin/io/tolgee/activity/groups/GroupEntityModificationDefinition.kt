package io.tolgee.activity.groups

import io.tolgee.activity.data.RevisionType
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class GroupEntityModificationDefinition<T : Any>(
  val entityClass: KClass<T>,
  val revisionTypes: List<RevisionType>,
  val modificationProps: List<KProperty1<T, *>>? = null,
  val allowedValues: Map<KProperty1<T, *>, Any?>? = null,
  val deniedValues: Map<KProperty1<T, *>, Any?>? = null,
  val countInView: Boolean = false,
)
