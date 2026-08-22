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
   * How many projects each of these installs is enabled for, in one query, so a list of installs
   * does not fan out into one count per install.
   */
  @Query(
    """
    select e.appInstall.id, count(e)
    from AppEnabledForProject e
    where e.appInstall.id in :installIds
    group by e.appInstall.id
    """,
  )
  fun countEnabledProjectsByInstallIds(
    @Param("installIds") installIds: Collection<Long>,
  ): List<Array<Any>>

  /**
   * Disables an app in every non-owner project that can no longer reach it - used after a change to
   * the app's availability set, so it stops running wherever it could only be reached through an
   * availability entry that is now gone, while staying enabled in the owner's own projects and in
   * organizations the app is still available to.
   */
  @Query(
    """
    delete from AppEnabledForProject e
    where e.appInstall.app.id = :appEntityId
      and e.project.organizationOwner.id <> e.appInstall.app.organization.id
      and not exists (
        select 1 from AppAvailability av
        where av.app.id = :appEntityId
          and (av.organization is null or av.organization.id = e.project.organizationOwner.id)
      )
    """,
  )
  @Modifying(clearAutomatically = true)
  fun disableWhereNoLongerAvailable(
    @Param("appEntityId") appEntityId: Long,
  )
}
