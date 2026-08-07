package io.tolgee.repository.apps

import io.tolgee.model.Organization
import io.tolgee.model.apps.AppAvailableForOrganization
import io.tolgee.model.apps.AppInstall
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface AppAvailableForOrganizationRepository : JpaRepository<AppAvailableForOrganization, Long> {
  fun findByAppInstallIdAndOrganizationId(
    appInstallId: Long,
    organizationId: Long,
  ): AppAvailableForOrganization?

  @Query(
    """
    select a.organization from AppAvailableForOrganization a
    where a.appInstall.id = :appInstallId
    order by a.organization.name
    """,
  )
  fun findOrganizationsByAppInstallId(appInstallId: Long): List<Organization>

  @Query(
    """
    select a.appInstall from AppAvailableForOrganization a
    where a.organization.id = :organizationId and a.appInstall.organization is null
    """,
  )
  fun findNativeInstallsByOrganizationId(organizationId: Long): List<AppInstall>

  fun deleteByAppInstallId(appInstallId: Long)
}
