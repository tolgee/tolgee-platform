package io.tolgee.events

import io.tolgee.activity.iterceptor.PreCommitEventPublisher
import org.springframework.context.ApplicationEvent
import org.springframework.core.ResolvableType
import org.springframework.core.ResolvableTypeProvider

class OnEntityPreDelete<T : Any>(
  @Suppress("PROPERTY_HIDES_JAVA_FIELD")
  override val source: PreCommitEventPublisher,
  override val entity: T?,
) : ApplicationEvent(source),
  EntityPreCommitEvent<T>,
  ResolvableTypeProvider {
  override fun getResolvableType(): ResolvableType = resolvableTypeFor(OnEntityPreDelete::class.java, entity)
}
