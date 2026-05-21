package io.tolgee.events

import io.tolgee.activity.iterceptor.PreCommitEventPublisher

interface EntityPreCommitEvent<T : Any> {
  val source: PreCommitEventPublisher
  val entity: T?
}
