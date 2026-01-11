package io.tolgee.events

import io.tolgee.activity.iterceptor.PreCommitEventPublisher
import org.springframework.context.ApplicationEvent

class OnEntityPreUpdate(
  override val source: PreCommitEventPublisher,
  override val entity: Any?,
  val previousState: Array<out Any>?,
  val propertyNames: Array<out String>?,
) : ApplicationEvent(source),
  EntityPreCommitEvent
