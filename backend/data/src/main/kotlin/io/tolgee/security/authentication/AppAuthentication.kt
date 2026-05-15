package io.tolgee.security.authentication

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.model.apps.AppInstall

/**
 * Authentication populated when a request bears app credentials, either:
 *  - a user-context JWT (audience `tg.app`) — `projectId` is bound to the JWT and `userAccount`
 *    is the iframe user, OR
 *  - an `X-API-Key: tgapps_…` / `Authorization: Basic …` app secret — `projectId` is null
 *    (downstream code resolves it from the URL and verifies enablement per request),
 *    `userAccount` is the install's author for audit purposes, and `actingAsUserAccount` is
 *    optionally set from the `X-Tolgee-Act-As-User-Id` header.
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
