package io.tolgee.events

import org.springframework.context.ApplicationEvent

class OnUserCountChanged(
  source: Any,
) : ApplicationEvent(source)
