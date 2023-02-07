package io.tolgee.events

import io.tolgee.activity.iterceptor.PreCommitEventPublisher
import org.springframework.context.ApplicationEvent

class OnEntityPreDelete(
  val source: PreCommitEventPublisher,
  val entity: Any?
) : ApplicationEvent(source)
