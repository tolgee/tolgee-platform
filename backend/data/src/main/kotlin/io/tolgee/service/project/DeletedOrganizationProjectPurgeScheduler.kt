package io.tolgee.service.project

import io.tolgee.component.LockingProvider
import io.tolgee.component.SchedulingManager
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import io.tolgee.util.runSentryCatching
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.time.Duration

/**
 * Hard-deletes projects whose owning organization was soft-deleted.
 *
 * Organization soft-delete only marks the projects deleted (so they disappear from listings
 * immediately); this scheduler reclaims their data afterwards. It also cleans up projects that
 * were orphaned under an organization deleted before that cascade existed.
 */
@Component
class DeletedOrganizationProjectPurgeScheduler(
  private val projectService: ProjectService,
  @Lazy
  private val projectHardDeletingService: ProjectHardDeletingService,
  private val lockingProvider: LockingProvider,
  private val transactionManager: PlatformTransactionManager,
  private val schedulingManager: SchedulingManager,
  private val internalProperties: InternalProperties,
) : Logging {
  @EventListener(ApplicationReadyEvent::class)
  fun schedulePurge() {
    if (!internalProperties.orphanProjectPurgeEnabled) {
      logger.info("Orphan project purge is disabled, skipping scheduling")
      return
    }
    schedulingManager.scheduleWithFixedDelay(::purge, PURGE_PERIOD)
    logger.info("Scheduled orphan project purge task with period: {}", PURGE_PERIOD)
  }

  fun purge() {
    lockingProvider.withLockingIfFree(PURGE_LOCK_NAME, PURGE_LOCK_LEASE_TIME) {
      runSentryCatching {
        purgeProjectsOfDeletedOrganizations()
      }
    }
  }

  private fun purgeProjectsOfDeletedOrganizations() {
    logger.info("Running orphan project purge")
    var totalPurged = 0
    do {
      val batch =
        executeInNewTransaction(transactionManager) {
          projectService.findIdsInDeletedOrganizations(PageRequest.of(0, BATCH_SIZE))
        }

      if (batch.isEmpty) break

      batch.content.forEach { projectId ->
        executeInNewTransaction(transactionManager) {
          projectService.findIncludingDeleted(projectId)?.let {
            projectHardDeletingService.hardDeleteProject(it)
          }
        }
      }
      totalPurged += batch.numberOfElements
    } while (batch.hasNext())

    logger.info("Orphan project purge finished, purged {} projects", totalPurged)
  }

  companion object {
    private const val PURGE_LOCK_NAME = "orphan_project_purge_lock"
    private const val BATCH_SIZE = 50
    private val PURGE_PERIOD = Duration.ofDays(1)
    private val PURGE_LOCK_LEASE_TIME = Duration.ofMinutes(10)
  }
}
