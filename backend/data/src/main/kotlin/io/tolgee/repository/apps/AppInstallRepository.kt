package io.tolgee.repository.apps

import io.tolgee.model.Organization
import io.tolgee.model.apps.AppInstall
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface AppInstallRepository : JpaRepository<AppInstall, Long> {
  fun findAllByOrganizationId(organizationId: Long): List<AppInstall>

  fun findByOrganizationIdAndId(
    organizationId: Long,
    id: Long,
  ): AppInstall?

  fun findByOrganizationIdAndAppId(
    organizationId: Long,
    appId: String,
  ): AppInstall?

  /**
   * Installs of server-wide apps that some *other* organization owns — the ones [organizationId]'s
   * projects may enable on top of its own installs.
   */
  @Query(
    """
    select i from AppInstall i
    where i.app.availableToAllOrganizations = true and i.organization.id <> :organizationId
    order by i.name
    """,
  )
  fun findAvailableToOtherOrganizations(organizationId: Long): List<AppInstall>

  /**
   * The app is fetched eagerly because app-token authentication reads its token cutoff from the
   * servlet filter, outside any session — a lazy proxy there fails the request instead of
   * authenticating it.
   */
  @Query("select i from AppInstall i join fetch i.app where i.id = :id")
  fun findWithAppById(id: Long): AppInstall?

  @Query("select count(i) from AppInstall i where i.app.id = :appId")
  fun countByRegisteredAppId(appId: Long): Long

  @Query("select i from AppInstall i where i.app.id = :appId")
  fun findAllByRegisteredAppId(appId: Long): List<AppInstall>

  fun countByOrganizationId(organizationId: Long): Long

  /** The organizations that currently have the app installed, for the owner's installations view. */
  @Query(
    """
    select distinct i.organization from AppInstall i where i.app.id = :appEntityId
      and (:search is null
        or lower(i.organization.name) like lower(concat('%', cast(:search as text), '%'))
        or lower(i.organization.slug) like lower(concat('%', cast(:search as text), '%')))
    """,
    countQuery = """
    select count(distinct i.organization) from AppInstall i where i.app.id = :appEntityId
      and (:search is null
        or lower(i.organization.name) like lower(concat('%', cast(:search as text), '%'))
        or lower(i.organization.slug) like lower(concat('%', cast(:search as text), '%')))
    """,
  )
  fun findInstallingOrganizations(
    appEntityId: Long,
    search: String?,
    pageable: Pageable,
  ): Page<Organization>
}
