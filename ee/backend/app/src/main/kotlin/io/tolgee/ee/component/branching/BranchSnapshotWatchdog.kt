package io.tolgee.ee.component.branching

import io.tolgee.ee.repository.branching.BranchRepository
import io.tolgee.model.enums.SnapshotStatus
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.Date

@Component
class BranchSnapshotWatchdog(
  private val branchRepository: BranchRepository,
) : Logging {
  @Scheduled(fixedDelayString = "\${tolgee.branching.snapshot-watchdog-interval-ms:60000}")
  @Transactional
  fun checkStuck() {
    val cutoff = Date(System.currentTimeMillis() - 30 * 60 * 1000L)
    val stuck = branchRepository.findStuckBranches(cutoff)
    if (stuck.isNotEmpty()) {
      logger.warn("Watchdog marking ${stuck.size} stuck snapshot(s) as FAILED")
    }
    stuck.forEach { branch ->
      branch.snapshotStatus = SnapshotStatus.FAILED
      branch.writeLocked = false
      branch.snapshotErrorMessage = "Snapshot timed out"
      branchRepository.save(branch)
    }
  }
}
