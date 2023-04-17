package io.tolgee.events

import io.tolgee.activity.iterceptor.PreCommitEventPublisher

interface EntityPreCommitEvent {
  val source: PreCommitEventPublisher
  val entity: Any?
}
