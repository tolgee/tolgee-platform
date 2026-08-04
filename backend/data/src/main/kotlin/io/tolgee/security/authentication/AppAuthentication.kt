package io.tolgee.security.authentication

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.model.apps.AppInstall

/**
 * Authentication populated when a request bears an app JWT (audience `tg.app`), either:
 *  - a **user-context** token — `projectId` is bound to the JWT, `userAccount` is the iframe user,
 *    and `isInstallContext` is false, OR
 *  - an **install-context** token (minted at the OAuth token endpoint from the app's client
 *    credentials) — `projectId` is resolved from the request URL and enablement is verified per
 *    request, `userAccount` is the install's author for audit purposes, `isInstallContext` is true,
 *    and `actingAsUserAccount` is optionally set from the `X-Tolgee-Act-As-User-Id` header.
 */
class AppAuthentication(
  credentials: Any?,
  userAccount: UserAccountDto,
  val appInstall: AppInstall,
  val projectId: Long?,
  val isInstallContext: Boolean,
  actingAsUserAccount: UserAccountDto? = null,
) : TolgeeAuthentication(
    credentials = credentials,
    deviceId = null,
    userAccount = userAccount,
    actingAsUserAccount = actingAsUserAccount,
    isReadOnly = false,
    isSuperToken = false,
  )
