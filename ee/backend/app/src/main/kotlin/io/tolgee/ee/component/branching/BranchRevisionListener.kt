package io.tolgee.ee.component.branching

import io.tolgee.ee.repository.branching.BranchRepository
import io.tolgee.events.OnProjectActivityEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class BranchRevisionListener(
  private val branchRepository: BranchRepository,
) {
  @EventListener
  fun onActivity(event: OnProjectActivityEvent) {
    val branchIds =
      event.modifiedEntities.values
        .flatMap { it.values }
        .mapNotNull { it.branchId }
        .toSet()

    branchIds.forEach { branchId ->
      val branch = branchRepository.findById(branchId).orElse(null) ?: return@forEach
      branch.revision++
    }
  }
}
