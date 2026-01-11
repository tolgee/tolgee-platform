package io.tolgee.events

import io.tolgee.model.key.Key
import org.springframework.context.ApplicationEvent

class OnKeyPrePersist(
  source: Any,
  val key: Key,
) : ApplicationEvent(source)
