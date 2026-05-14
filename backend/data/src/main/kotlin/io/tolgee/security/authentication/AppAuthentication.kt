package io.tolgee.security.authentication

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.model.apps.AppInstall

/**
 * Authentication populated when a request bears an app-issued JWT (audience `tg.app`).
 *
 * Extends [TolgeeAuthentication] so that all downstream code that reads `.principal`,
 * `.credentials`, or `authenticationFacade.authentication` continues to work transparently.
 * The PoC distinguishing data is `appInstall` and `projectId` — the install determines
 * the scope ceiling, and `projectId` is the project the token is bound to.
 *
 * Admin-bypass is suppressed for this authentication type — see
 * `ProjectContextService.canUseAdminPermissions` and `AuthenticationFacade.isAppAuth`.
 */
class AppAuthentication(
  credentials: Any?,
  userAccount: UserAccountDto,
  val appInstall: AppInstall,
  val projectId: Long,
) : TolgeeAuthentication(
    credentials = credentials,
    deviceId = null,
    userAccount = userAccount,
    actingAsUserAccount = null,
    isReadOnly = false,
    isSuperToken = false,
  )
