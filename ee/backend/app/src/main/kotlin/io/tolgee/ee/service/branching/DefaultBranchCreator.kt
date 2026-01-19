package io.tolgee.ee.service.branching

import io.tolgee.model.Project
import io.tolgee.model.branching.Branch
import io.tolgee.service.project.ProjectService
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class DefaultBranchCreator(
  private val projectService: ProjectService,
  private val entityManager: EntityManager,
) {
  @Transactional
  fun create(projectId: Long): Branch {
    val project = projectService.get(projectId)
    return create(project)
  }

  @Transactional
  fun create(project: Project): Branch {
    val branch = Branch.createMainBranch(project)
    entityManager.persist(branch)
    entityManager.flush()

    entityManager
      .createQuery(
        """
        UPDATE Key k
        SET k.branch = :branch
        WHERE k.project.id = :projectId
        """,
      ).setParameter("branch", branch)
      .setParameter("projectId", project.id)
      .executeUpdate()

    return branch
  }
}
