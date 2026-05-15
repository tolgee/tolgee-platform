package io.tolgee.repository.apps

import io.tolgee.model.apps.AppInstall
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
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

  fun findByClientSecretHash(clientSecretHash: String): AppInstall?

  fun findByClientId(clientId: String): AppInstall?
}
