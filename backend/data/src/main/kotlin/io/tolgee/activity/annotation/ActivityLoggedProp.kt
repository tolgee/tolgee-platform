package io.tolgee.activity.annotation

import io.tolgee.activity.propChangesProvider.DefaultPropChangesProvider
import io.tolgee.activity.propChangesProvider.PropChangesProvider
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
annotation class ActivityLoggedProp(
  val modificationProvider: KClass<out PropChangesProvider> = DefaultPropChangesProvider::class,
)
