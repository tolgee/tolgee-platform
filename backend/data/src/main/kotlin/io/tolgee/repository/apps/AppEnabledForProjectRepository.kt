package io.tolgee.repository.apps

import io.tolgee.model.Project
import io.tolgee.model.apps.AppEnabledForProject
import io.tolgee.model.apps.AppInstall
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface AppEnabledForProjectRepository : JpaRepository<AppEnabledForProject, Long> {
  fun findByProjectIdAndAppInstallId(
    projectId: Long,
    appInstallId: Long,
  ): AppEnabledForProject?

  fun findAllByProjectId(projectId: Long): List<AppEnabledForProject>

  @Query(
    """
    select e.appInstall from AppEnabledForProject e
    where e.project.id = :projectId
    order by e.appInstall.app.name
    """,
  )
  fun findEnabledInstallsByProjectId(
    @Param("projectId") projectId: Long,
  ): List<AppInstall>

  @Query(
    """
    select p from AppEnabledForProject e
    join e.project p
    join fetch p.organizationOwner o
    where e.appInstall.id = :appInstallId
      and p.deletedAt is null
      and o.deletedAt is null
    order by p.name, p.id
    """,
  )
  fun findEnabledProjectsByAppInstallId(
    @Param("appInstallId") appInstallId: Long,
  ): List<Project>

  fun deleteByAppInstallId(appInstallId: Long)

  fun deleteByProjectId(projectId: Long)

  /**
   * Disables an app in every project whose organization does not own it — used when a server admin
   * withdraws the app's server-wide availability, so it stops running everywhere it could only be
   * reached through that offer while staying enabled in the owner's own projects.
   */
  @Query(
    """
    delete from AppEnabledForProject e
    where e.appInstall.app.id = :appEntityId
      and e.project.organizationOwner.id <> e.appInstall.app.organization.id
    """,
  )
  @Modifying(clearAutomatically = true)
  fun deleteByAppIdAndProjectOrganizationNotOwner(
    @Param("appEntityId") appEntityId: Long,
  )
}
