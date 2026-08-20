package io.tolgee.repository.apps

import io.tolgee.model.Organization
import io.tolgee.model.apps.AppInstall
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface AppInstallRepository : JpaRepository<AppInstall, Long> {
  fun findAllByOrganizationId(organizationId: Long): List<AppInstall>

  fun findByOrganizationIdAndId(
    organizationId: Long,
    id: Long,
  ): AppInstall?

  @Query(
    """
    select i from AppInstall i
    where i.organization.id = :organizationId and i.app.appId = :appId
    """,
  )
  fun findByOrganizationIdAndManifestAppId(
    @Param("organizationId") organizationId: Long,
    @Param("appId") appId: String,
  ): AppInstall?

  /**
   * The app is fetched eagerly because app-token authentication reads its token cutoff from the
   * servlet filter, outside any session — a lazy proxy there fails the request instead of
   * authenticating it.
   */
  @Query("select i from AppInstall i join fetch i.app where i.id = :id")
  fun findWithAppById(
    @Param("id") id: Long,
  ): AppInstall?

  @Query("select count(i) from AppInstall i where i.app.id = :appEntityId")
  fun countByRegisteredAppId(
    @Param("appEntityId") appEntityId: Long,
  ): Long

  @Query("select i from AppInstall i where i.app.id = :appEntityId")
  fun findAllByRegisteredAppId(
    @Param("appEntityId") appEntityId: Long,
  ): List<AppInstall>

  fun countByOrganizationId(organizationId: Long): Long

  /**
   * The organizations that currently have the app installed, for the owner's installations view.
   * Ordered here (name, then id for ties) because a paged select without a total order can repeat
   * or drop rows across pages.
   */
  @Query(
    """
    select distinct i.organization from AppInstall i where i.app.id = :appEntityId
      and (:search is null
        or lower(i.organization.name) like lower(concat('%', cast(:search as text), '%'))
        or lower(i.organization.slug) like lower(concat('%', cast(:search as text), '%')))
    order by i.organization.name, i.organization.id
    """,
    countQuery = """
    select count(distinct i.organization) from AppInstall i where i.app.id = :appEntityId
      and (:search is null
        or lower(i.organization.name) like lower(concat('%', cast(:search as text), '%'))
        or lower(i.organization.slug) like lower(concat('%', cast(:search as text), '%')))
    """,
  )
  fun findInstallingOrganizations(
    @Param("appEntityId") appEntityId: Long,
    @Param("search") search: String?,
    pageable: Pageable,
  ): Page<Organization>
}
