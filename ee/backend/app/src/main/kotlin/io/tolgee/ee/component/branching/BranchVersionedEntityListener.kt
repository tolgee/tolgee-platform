package io.tolgee.ee.component.branching

import io.tolgee.events.OnEntityPreDelete
import io.tolgee.events.OnEntityPrePersist
import io.tolgee.events.OnEntityPreUpdate
import io.tolgee.model.branching.BranchVersionedEntity
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class BranchVersionedEntityListener(
  private val branchRevisionUpdater: BranchRevisionUpdater,
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
      event.previousState?.let {
        event.propertyNames?.zip(it)
      }?.toMap()
    )
  }

  fun onChange(entity: Any?, oldState: Map<String, Any>? = null) {
    if (entity == null) return
    if (entity !is BranchVersionedEntity) return

    val branchId = entity.resolveBranchId() ?: return

    if (oldState == null || entity.isDifferent(oldState)) {
      branchRevisionUpdater.update(branchId)
    }
  }
}
