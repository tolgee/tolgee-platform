package io.tolgee.security.authentication

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.model.UserAccount
import io.tolgee.model.apps.AppInstall

/**
 * Authentication for an app JWT, in one of three contexts:
 *  - **install-context**: `userAccount` is the install's [AppInstall.principal]; [appInstall] is set.
 *  - **user-context**: `userAccount` is the iframe user; [tokenProjectId] is set.
 *  - **app-level** ([isAppLevel]): identifies the app itself for app-level operations (installation
 *    discovery). There is no install and no per-app person, so `userAccount` is a synthetic,
 *    permission-less principal; the [AppAccessInterceptor] confines these tokens to
 *    [AllowAppLevelAccess] endpoints, where only [appId] is read.
 *
 * [actsForUserId] is the person the install acts *for* (the `X-Tolgee-Act-As-User-Id` header),
 * resolved and membership-checked lazily by `ProjectContextService` — never in the filter, so a route
 * that binds no project cannot probe which user ids exist. [boundProjectId] is set by
 * `ProjectContextService`.
 */
class AppAuthentication(
  credentials: Any?,
  userAccount: UserAccountDto,
  private val appInstallOrNull: AppInstall?,
  val appId: Long,
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

  val isAppLevel: Boolean
    get() = appInstallOrNull == null

  /** The install this token acts as. Never call it on an app-level token — it has none. */
  val appInstall: AppInstall
    get() = appInstallOrNull ?: throw IllegalStateException("An app-level token is not bound to an install")

  companion object {
    fun appLevel(
      credentials: Any?,
      appId: Long,
      isReadOnly: Boolean,
    ): AppAuthentication =
      AppAuthentication(
        credentials = credentials,
        userAccount = syntheticAppPrincipal(appId),
        appInstallOrNull = null,
        appId = appId,
        tokenProjectId = null,
        isInstallContext = false,
        isReadOnly = isReadOnly,
      )

    /**
     * A stand-in principal so an app-level token satisfies the shared user-centric auth chain. It
     * carries no real identity: the interceptor allows app-level tokens only on app-level endpoints,
     * which read [appId] and never this principal, and app tokens never reach a permission check.
     */
    private fun syntheticAppPrincipal(appId: Long): UserAccountDto =
      UserAccountDto(
        name = "App #$appId",
        username = "app-$appId",
        domain = null,
        role = UserAccount.Role.USER,
        id = appId,
        needsSuperJwt = false,
        avatarHash = null,
        deleted = false,
        tokensValidNotBefore = null,
        emailVerified = true,
        thirdPartyAuth = null,
        ssoRefreshToken = null,
        ssoSessionExpiry = null,
      )
  }
}
