package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope
import io.tolgee.model.oauth2.OAuth2Grant
import io.tolgee.security.oauth2.OAuth2Constants
import java.util.Date
import java.util.UUID

/**
 * OAuth grants covering the states `ConnectedAppService` distinguishes: a connected grant needs a
 * consented `projectSelection` and a live refresh token together — a pending consent (neither set)
 * or a lapsed refresh token (expired) each silently drop the grant from the connected-apps listing.
 * Also seeds a second user account for foreign-grant / cross-user checks. Each test adds only the
 * grants it asserts on.
 */
class ConnectedAppsTestData(
  primaryUsername: String = "test_username",
) : BaseTestData(userName = primaryUsername) {
  val otherUserAccountBuilder: UserAccountBuilder = root.addUserAccount { username = "connected-apps-other" }
  val otherUser: UserAccount get() = otherUserAccountBuilder.self

  fun addConnectedGrant(
    accountBuilder: UserAccountBuilder = userAccountBuilder,
    clientId: String = OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID,
    projectIds: List<Long>? = null,
    scopeValues: List<String> = listOf(Scope.TRANSLATIONS_VIEW.value),
  ): OAuth2Grant =
    addGrant(accountBuilder, clientId, refreshTokenExpired = false) {
      projectSelection = projectIds?.joinToString(",") ?: OAuth2Constants.ALL_PROJECTS
      requestedScopeValues = scopeValues
      issuedTokenScopeValues = requestedScopeValues
    }

  fun addPendingGrant(
    accountBuilder: UserAccountBuilder = userAccountBuilder,
    clientId: String = OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID,
  ): OAuth2Grant = addGrant(accountBuilder, clientId, refreshTokenExpired = false) {}

  fun addLapsedGrant(
    accountBuilder: UserAccountBuilder = userAccountBuilder,
    clientId: String = OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID,
    scopeValues: List<String> = listOf(Scope.TRANSLATIONS_VIEW.value),
  ): OAuth2Grant =
    addGrant(accountBuilder, clientId, refreshTokenExpired = true) {
      projectSelection = OAuth2Constants.ALL_PROJECTS
      requestedScopeValues = scopeValues
      issuedTokenScopeValues = requestedScopeValues
    }

  private fun addGrant(
    accountBuilder: UserAccountBuilder,
    clientId: String,
    refreshTokenExpired: Boolean,
    configure: OAuth2Grant.() -> Unit,
  ): OAuth2Grant =
    accountBuilder
      .addOAuth2Grant {
        this.clientId = clientId
        refreshTokenHash = "hash-" + UUID.randomUUID()
        refreshTokenExpiresAt = refreshTokenExpiry(refreshTokenExpired)
        accessTokenIssuedAt = Date()
        configure()
      }.self

  private fun refreshTokenExpiry(expired: Boolean): Date {
    if (expired) return Date(Date().time - 1000)
    return Date(Date().time + 86_400_000)
  }
}
