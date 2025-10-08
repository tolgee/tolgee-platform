package io.tolgee.ee.component.branching

import io.tolgee.ee.repository.BranchRepository
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class BranchRevisionUpdater(
  private val branchRepository: BranchRepository,
) {

  @Async
  @Transactional
  fun update(branchId: Long) {
    val branch = branchRepository.findByIdOrNull(branchId)
    if (branch == null) return
    branch.revision++
  }
}
