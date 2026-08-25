package io.tolgee.security.authentication

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.model.apps.AppInstall

/**
 * Authentication for an app JWT. Install-context: `userAccount` is the install's [AppInstall.principal].
 * User-context: `userAccount` is the iframe user, [tokenProjectId] set. [actsForUserAccount] is its own
 * field (not the base `actingAsUserAccount`, whose direction is inverted). [boundProjectId] is set by
 * `ProjectContextService`.
 */
class AppAuthentication(
  credentials: Any?,
  userAccount: UserAccountDto,
  val appInstall: AppInstall,
  val tokenProjectId: Long?,
  val isInstallContext: Boolean,
  isReadOnly: Boolean,
  val actsForUserAccount: UserAccountDto? = null,
) : TolgeeAuthentication(
    credentials = credentials,
    deviceId = null,
    userAccount = userAccount,
    actingAsUserAccount = null,
    isReadOnly = isReadOnly,
    isSuperToken = false,
  ) {
  var boundProjectId: Long? = null
}
