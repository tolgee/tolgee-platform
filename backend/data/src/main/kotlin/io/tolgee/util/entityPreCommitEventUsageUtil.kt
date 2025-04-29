package io.tolgee.util

import io.tolgee.events.EntityPreCommitEvent
import io.tolgee.events.OnEntityPreDelete
import io.tolgee.events.OnEntityPrePersist

fun EntityPreCommitEvent.getUsageIncreaseAmount(): Long {
  return when (this) {
    is OnEntityPrePersist -> 1
    is OnEntityPreDelete -> -1
    else -> 0
  }
}
