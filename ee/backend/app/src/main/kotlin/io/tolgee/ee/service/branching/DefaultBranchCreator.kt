package io.tolgee.ee.service.branching

import io.tolgee.model.Project
import io.tolgee.model.branching.Branch
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class DefaultBranchCreator(
  private val entityManager: EntityManager,
) {
  @Transactional
  fun create(project: Project): Branch {
    val branch = Branch.createMainBranch(project)
    entityManager.persist(branch)
    return branch
  }
}
