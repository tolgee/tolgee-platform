package io.tolgee.events

import org.springframework.context.ApplicationEvent

class OnUserCountChanged(
  val decrease: Boolean,
  source: Any,
) : ApplicationEvent(source)
