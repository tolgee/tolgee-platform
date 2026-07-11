package io.tolgee.events

import io.tolgee.activity.iterceptor.PreCommitEventPublisher
import org.springframework.context.ApplicationEvent
import org.springframework.core.ResolvableType
import org.springframework.core.ResolvableTypeProvider

class OnEntityPreUpdate<T : Any>(
  override val source: PreCommitEventPublisher,
  override val entity: T?,
  val previousState: Array<out Any>?,
  val propertyNames: Array<out String>?,
) : ApplicationEvent(source),
  EntityPreCommitEvent<T>,
  ResolvableTypeProvider {
  override fun getResolvableType(): ResolvableType = resolvableTypeFor(OnEntityPreUpdate::class.java, entity)
}
