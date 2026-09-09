package io.tolgee.service.apps

import io.tolgee.model.Project
import io.tolgee.model.apps.AppEnabledForProject
import io.tolgee.model.apps.AppInstall
import io.tolgee.repository.apps.AppEnabledForProjectRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class AppEnablementInserter(
  private val appEnabledForProjectRepository: AppEnabledForProjectRepository,
  private val entityManager: EntityManager,
) {
  /**
   * Runs in its own transaction so that a duplicate-key violation — which Hibernate answers by
   * marking the *current* transaction rollback-only — dooms only this insert. The caller catches
   * the violation and keeps its own transaction committable.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun insert(
    appInstallId: Long,
    projectId: Long,
  ) {
    appEnabledForProjectRepository.saveAndFlush(
      AppEnabledForProject().apply {
        this.appInstall = entityManager.getReference(AppInstall::class.java, appInstallId)
        this.project = entityManager.getReference(Project::class.java, projectId)
      },
    )
  }
}
