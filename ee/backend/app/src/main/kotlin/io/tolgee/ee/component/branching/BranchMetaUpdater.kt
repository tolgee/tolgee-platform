package io.tolgee.ee.component.branching

import io.tolgee.component.CurrentDateProvider
import io.tolgee.ee.repository.branching.BranchRepository
import io.tolgee.model.key.Key
import jakarta.transaction.Transactional
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class BranchMetaUpdater(
  private val branchRepository: BranchRepository,
  private val currentDateProvider: CurrentDateProvider,
) {

  @Async
  @Transactional
  fun snapshot(key: Key) {
    key.branch?.let { b ->
      val branch = branchRepository.findById(b.id).orElse(null) ?: return@let
      branch.revision++
      key.cascadeUpdatedAt = currentDateProvider.date
    }
  }
}
