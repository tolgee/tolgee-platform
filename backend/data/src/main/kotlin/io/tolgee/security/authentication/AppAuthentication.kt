package io.tolgee.security.authentication

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.model.UserAccount
import io.tolgee.model.apps.AppInstall

class AppAuthentication(
  credentials: Any?,
  userAccount: UserAccountDto,
  private val appInstallOrNull: AppInstall?,
  val appId: Long,
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
        isInstallContext = false,
        isReadOnly = isReadOnly,
      )

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
