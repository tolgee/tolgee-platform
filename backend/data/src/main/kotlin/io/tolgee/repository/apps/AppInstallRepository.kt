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

  @Query("select count(i) from AppInstall i where i.app.id = :appId")
  fun countByRegisteredAppId(appId: Long): Long
}
