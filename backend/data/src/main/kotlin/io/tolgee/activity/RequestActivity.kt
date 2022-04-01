package io.tolgee.activity

import io.tolgee.activity.activities.ActivityManager
import kotlin.reflect.KClass

annotation class RequestActivity(
  val manager: KClass<out ActivityManager>
)
