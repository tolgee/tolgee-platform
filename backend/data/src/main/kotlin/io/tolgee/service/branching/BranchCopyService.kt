package io.tolgee.service.branching

import io.tolgee.model.branching.Branch

interface BranchCopyService {
  /**
   * Copies keys + translations (+ labels) from source branch into target branch within the same project.
   */
  fun copy(
    projectId: Long,
    sourceBranch: Branch,
    targetBranch: Branch,
  )
}
