package io.tolgee.repository.apps

import io.tolgee.model.apps.AppEnabledForProject
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface AppEnabledForProjectRepository : JpaRepository<AppEnabledForProject, Long> {
  fun findByProjectIdAndAppInstallId(
    projectId: Long,
    appInstallId: Long,
  ): AppEnabledForProject?

  fun findAllByProjectId(projectId: Long): List<AppEnabledForProject>

  fun findAllByAppInstallId(appInstallId: Long): List<AppEnabledForProject>

  fun deleteByAppInstallId(appInstallId: Long)
}
