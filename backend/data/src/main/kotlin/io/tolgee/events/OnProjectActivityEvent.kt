package io.tolgee.events

import io.tolgee.activity.ActivityHolder
import org.springframework.context.ApplicationEvent

class OnProjectActivityEvent(
  val source: ActivityHolder,
) : ApplicationEvent(source)
