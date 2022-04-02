package io.tolgee.activity

import io.tolgee.activity.activities.common.Activity
import kotlin.reflect.KClass

annotation class RequestActivity(
  val activity: KClass<out Activity>
)
