package io.tolgee.repository.apps

import io.tolgee.model.apps.AppEnabledForProject
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface AppEnabledForProjectRepository : JpaRepository<AppEnabledForProject, Long> {
  fun findByProjectIdAndAppInstallId(
    projectId: Long,
    appInstallId: Long,
  ): AppEnabledForProject?

  fun findAllByProjectId(projectId: Long): List<AppEnabledForProject>

  fun deleteByAppInstallId(appInstallId: Long)

  fun deleteByAppInstallIdAndProjectOrganizationOwnerId(
    appInstallId: Long,
    organizationId: Long,
  )

  @Query(
    """
    select e from AppEnabledForProject e
    where e.appInstall.id = :appInstallId
      and not exists (
        select a.id from AppAvailableForOrganization a
        where a.appInstall.id = :appInstallId
          and a.organization.id = e.project.organizationOwner.id
      )
    """,
  )
  fun findAllWithoutExplicitOrganizationAvailability(appInstallId: Long): List<AppEnabledForProject>
}
