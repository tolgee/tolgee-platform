package io.tolgee.ee.service.branching

import io.tolgee.exceptions.NotFoundException
import io.tolgee.repository.ProjectRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectBranchingMigrationService(
  private val projectRepository: ProjectRepository,
  private val entityManager: EntityManager,
  private val defaultBranchCreator: DefaultBranchCreator,
) {
  @Transactional
  fun enableBranching(projectId: Long) {
    val project =
      projectRepository.findWithBranches(projectId)
        ?: throw NotFoundException()

    if (project.hasDefaultBranch()) {
      return
    }

    val defaultBranch = defaultBranchCreator.create(project)
    entityManager.flush()

    entityManager
      .createQuery(
        """
        update Key k
        set k.branch = :branch
        where k.project.id = :projectId
          and k.branch is null
        """,
      ).setParameter("branch", defaultBranch)
      .setParameter("projectId", projectId)
      .executeUpdate()

    entityManager
      .createQuery(
        """
        update Task t
        set t.branch = :branch
        where t.project.id = :projectId
          and t.branch is null
        """,
      ).setParameter("branch", defaultBranch)
      .setParameter("projectId", projectId)
      .executeUpdate()

    entityManager
      .createQuery(
        """
        update Import i
        set i.branch = :branch
        where i.project.id = :projectId
          and i.branch is null
        """,
      ).setParameter("branch", defaultBranch)
      .setParameter("projectId", projectId)
      .executeUpdate()

    entityManager
      .createQuery(
        """
        update LanguageStats ls
        set ls.branch = :branch
        where ls.branch is null
          and ls.language.project.id = :projectId
          and not exists (
            select 1
            from LanguageStats ls2
            where ls2.language = ls.language
              and ls2.branch = :branch
          )
    """,
      ).setParameter("branch", defaultBranch)
      .setParameter("projectId", projectId)
      .executeUpdate()

    entityManager
      .createQuery(
        """
        update ContentDeliveryConfig cdc
        set cdc.branch = :branch
        where cdc.project.id = :projectId
          and cdc.branch is null
        """,
      ).setParameter("branch", defaultBranch)
      .setParameter("projectId", projectId)
      .executeUpdate()
  }
}
