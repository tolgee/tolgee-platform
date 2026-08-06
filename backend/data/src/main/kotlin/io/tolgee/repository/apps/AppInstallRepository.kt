package io.tolgee.repository.apps

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

  fun findAllByOrganizationIsNull(pageable: Pageable): Page<AppInstall>

  fun findAllByOrganizationIsNullAndAvailableToAllOrganizationsIsTrue(): List<AppInstall>

  fun findByOrganizationIsNullAndId(id: Long): AppInstall?

  fun findByOrganizationIsNullAndAppId(appId: String): AppInstall?

  fun findByClientId(clientId: String): AppInstall?

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

  @Query("select i.app.id from AppInstall i where i.id = :installId and i.organization.id = :organizationId")
  fun findAppEntityId(
    organizationId: Long,
    installId: Long,
  ): Long?

  @Query("select i.app.id from AppInstall i where i.id = :installId and i.organization is null")
  fun findAppEntityIdOfNativeInstall(installId: Long): Long?

  @Query("select i.app.id from AppInstall i where i.id = :installId")
  fun findAppEntityIdOfInstall(installId: Long): Long?

  /** Null for a native install: the implicit join over a null organization matches nothing. */
  @Query("select i.organization.id from AppInstall i where i.id = :installId")
  fun findOrganizationIdOfInstall(installId: Long): Long?
}
