package io.tolgee.ee.service.branching

import io.tolgee.ee.repository.BranchRepository
import io.tolgee.events.OnBranchDeleted
import io.tolgee.service.key.KeyService
import io.tolgee.repository.KeyRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Service
class BranchCleanupService(
  private val keyRepository: KeyRepository,
  private val keyService: KeyService,
  private val branchRepository: BranchRepository,
) {
  companion object {
    private const val BATCH_SIZE = 1000
  }

  val logger: Logger by lazy {
    LoggerFactory.getLogger(javaClass)
  }

  /**
   * Async cleanup entry point. Deletes all keys and related data for the archived branch.
   * Uses KeyService to cascade-delete related entities in batches.
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
  @Async
  fun cleanupBranchAsync(event: OnBranchDeleted) {
    cleanupBranch(event.branch.id)
  }

  /**
   * Synchronous cleanup
   */
  fun cleanupBranch(branchId: Long) {
    val branch = branchRepository.findById(branchId).get()
    if (branch.archivedAt == null) return

    logger.warn("Cleaning up branch ${branch.id}")
    var page = 0
    while (true) {
      val idsPage = keyRepository.findIdsByProjectAndBranch(
        branch.project.id,
        branch.id,
        PageRequest.of(page, BATCH_SIZE)
      )
      if (idsPage.isEmpty) break
      val ids = idsPage.content
      if (ids.isNotEmpty()) {
        keyService.deleteMultiple(ids)
        logger.debug("Deleted ${ids.size} keys for branch ${branch.id}")
      }
      if (idsPage.numberOfElements < BATCH_SIZE) {
        break
      }
      page++
    }
  }
}
