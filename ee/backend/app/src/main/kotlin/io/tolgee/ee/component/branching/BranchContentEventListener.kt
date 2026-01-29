package io.tolgee.ee.component.branching

import io.tolgee.events.OnEntityCollectionPreUpdate
import io.tolgee.events.OnEntityPreDelete
import io.tolgee.events.OnEntityPrePersist
import io.tolgee.events.OnEntityPreUpdate
import io.tolgee.model.branching.BranchMergeableEntity
import io.tolgee.util.extractEntityId
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class BranchContentEventListener(
  private val branchRevisionUpdater: BranchMetaUpdater,
) {
  @EventListener
  fun onPersist(event: OnEntityPrePersist) {
    onChange(event.entity)
  }

  @EventListener
  fun onDelete(event: OnEntityPreDelete) {
    onChange(event.entity)
  }

  @EventListener
  fun onUpdate(event: OnEntityPreUpdate) {
    onChange(
      event.entity,
      event.previousState
        ?.let {
          event.propertyNames?.zip(it)
        }?.toMap(),
    )
  }

  @EventListener
  fun onCollectionUpdate(event: OnEntityCollectionPreUpdate) {
    onChange(event.entity)
  }

  fun onChange(
    entity: Any?,
    oldState: Map<String, Any>? = null,
  ) {
    if (entity == null) return
    if (entity !is BranchMergeableEntity<*, *>) return

    val key = entity.resolveKey() ?: return

    if (oldState == null || entity.isModified(oldState)) {
      val branchId = extractEntityId(key.branch)
      branchRevisionUpdater.snapshot(branchId)
    }
  }
}
