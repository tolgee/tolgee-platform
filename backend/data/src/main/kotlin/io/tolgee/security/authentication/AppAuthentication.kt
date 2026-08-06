package io.tolgee.security.authentication

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.model.apps.AppInstall

/**
 * Authentication populated when a request bears an app JWT (audience `tg.app`), either:
 *  - a **user-context** token — [tokenProjectId] is bound to the JWT, `userAccount` is the iframe
 *    user, and [isInstallContext] is false, OR
 *  - an **install-context** token (minted at the OAuth token endpoint from the app's client
 *    credentials) — not bound to a project, `userAccount` is the install's author for audit
 *    purposes, [isInstallContext] is true, and `actingAsUserAccount` is optionally set from the
 *    `X-Tolgee-Act-As-User-Id` header.
 *
 * [boundProjectId] is set by `ProjectContextService` once the request's project is known and its
 * enablement verified; permission resolution returns nothing for any other project.
 */
class AppAuthentication(
  credentials: Any?,
  userAccount: UserAccountDto,
  val appInstall: AppInstall,
  val tokenProjectId: Long?,
  val isInstallContext: Boolean,
  isReadOnly: Boolean,
  actingAsUserAccount: UserAccountDto? = null,
) : TolgeeAuthentication(
    credentials = credentials,
    deviceId = null,
    userAccount = userAccount,
    actingAsUserAccount = actingAsUserAccount,
    isReadOnly = isReadOnly,
    isSuperToken = false,
  ) {
  var boundProjectId: Long? = null
}
