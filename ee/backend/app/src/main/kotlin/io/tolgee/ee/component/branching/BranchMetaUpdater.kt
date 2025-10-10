package io.tolgee.ee.component.branching

import io.tolgee.component.CurrentDateProvider
import io.tolgee.repository.KeyRepository
import jakarta.transaction.Transactional
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class BranchMetaUpdater(
  private val keyRepository: KeyRepository,
  private val currentDateProvider: CurrentDateProvider,
) {

  @Async
  @Transactional
  fun snapshot(keyId: Long) {
    val key = keyRepository.findByIdWithBranch(keyId) ?: return
    key.branch!!.revision++
    key.cascadeUpdatedAt = currentDateProvider.date
  }
}
