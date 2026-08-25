package io.tolgee.security.authentication

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.model.apps.AppInstall

/**
 * Authentication for an app JWT. Install-context: `userAccount` is the install's [AppInstall.principal].
 * User-context: `userAccount` is the iframe user, [tokenProjectId] set. [actsForUserId] is the person the
 * install acts *for* (the `X-Tolgee-Act-As-User-Id` header), resolved and membership-checked lazily by
 * `ProjectContextService` — never in the filter, so a route that binds no project cannot probe which user
 * ids exist. [boundProjectId] is set by `ProjectContextService`.
 */
class AppAuthentication(
  credentials: Any?,
  userAccount: UserAccountDto,
  val appInstall: AppInstall,
  val tokenProjectId: Long?,
  val isInstallContext: Boolean,
  isReadOnly: Boolean,
  val actsForUserId: Long? = null,
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
