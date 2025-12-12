package io.tolgee.events

import io.tolgee.activity.iterceptor.PreCommitEventPublisher
import org.springframework.context.ApplicationEvent

class OnEntityCollectionPreUpdate(
  override val source: PreCommitEventPublisher,
  override val entity: Any?,
  previousCollection: MutableCollection<out Any?>?,
) : ApplicationEvent(source),
  EntityPreCommitEvent
