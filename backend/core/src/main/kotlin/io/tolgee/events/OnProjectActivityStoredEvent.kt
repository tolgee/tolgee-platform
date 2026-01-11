package io.tolgee.events

import io.tolgee.model.activity.ActivityRevision
import org.springframework.context.ApplicationEvent

class OnProjectActivityStoredEvent(
  source: Any,
  val activityRevision: ActivityRevision,
) : ApplicationEvent(source)
