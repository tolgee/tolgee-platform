package io.tolgee.ee.component.branching

import io.tolgee.ee.repository.branching.BranchRepository
import jakarta.transaction.Transactional
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class BranchMetaUpdater(
  private val branchRepository: BranchRepository,
) {
  /**
   * Updates branch revision for a key modification.
   * Takes branch ID instead of entity to avoid lazy loading issues during Hibernate event processing.
   */
  @Async
  @Transactional
  fun snapshot(branchId: Long?) {
    if (branchId == null) return
    val branch = branchRepository.findById(branchId).orElse(null) ?: return
    branch.revision++
  }
}
