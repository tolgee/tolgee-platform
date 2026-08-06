package io.tolgee.security.authentication

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.model.apps.AppInstall

/**
 * Authentication populated when a request bears an app JWT (audience `tg.app`), either:
 *  - a **user-context** token — [tokenProjectId] is bound to the JWT, `userAccount` is the iframe
 *    user, and [isInstallContext] is false, OR
 *  - an **install-context** token (minted at the OAuth token endpoint from the app's client
 *    credentials) — not bound to a project, [isInstallContext] is true, and `actingAsUserAccount`
 *    is optionally set from the `X-Tolgee-Act-As-User-Id` header.
 *
 * On the install-context path `userAccount` is the install's own [AppInstall.principal] — never the
 * person who registered it, so nothing operational depends on that person still existing or being
 * enabled. The principal holds no role, no membership and no project permission, so what the install
 * may do comes from [AppInstall.grantedScopes], optionally narrowed by `actingAsUserAccount` — whose
 * status and permissions do still count.
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
