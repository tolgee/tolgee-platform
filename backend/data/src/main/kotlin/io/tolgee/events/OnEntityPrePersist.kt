package io.tolgee.events

import io.tolgee.activity.iterceptor.PreCommitEventPublisher
import org.springframework.context.ApplicationEvent

class OnEntityPrePersist(
  override val source: PreCommitEventPublisher,
  override val entity: Any?,
) : ApplicationEvent(source),
  EntityPreCommitEvent
