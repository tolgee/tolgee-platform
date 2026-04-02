package io.tolgee.ee.component.branching

import io.tolgee.events.OnProjectActivityEvent
import jakarta.persistence.EntityManager
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class BranchRevisionListener(
  private val entityManager: EntityManager,
) {
  @EventListener
  fun onActivity(event: OnProjectActivityEvent) {
    val branchIds =
      event.modifiedEntities.values
        .flatMap { it.values }
        .mapNotNull { it.branchId }
        .toSet()

    if (branchIds.isEmpty()) return

    entityManager
      .createQuery("update Branch b set b.revision = b.revision + 1 where b.id in :ids")
      .setParameter("ids", branchIds)
      .executeUpdate()
  }
}
