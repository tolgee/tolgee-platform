package io.tolgee.dtos.cacheable

import io.tolgee.model.apps.AppInstall
import io.tolgee.model.enums.Scope
import java.io.Serializable

/**
 * The app-auth view of an [AppInstall]. Refers to the app by id ([appEntityId]) rather than
 * embedding it, so a force-revoke evicts only the one app snapshot, not every install of it.
 */
data class AppInstallDto(
  val id: Long,
  val appEntityId: Long,
  val organizationId: Long,
  val principalUserId: Long,
  val grantedScopes: Array<Scope>,
) : Serializable {
  companion object {
    fun fromEntity(install: AppInstall): AppInstallDto =
      AppInstallDto(
        id = install.id,
        appEntityId = install.app.id,
        organizationId = install.organization.id,
        principalUserId = install.principal.id,
        grantedScopes = install.grantedScopes,
      )
  }
}
