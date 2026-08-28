package io.tolgee.fixtures

import io.tolgee.component.KeyGenerator
import io.tolgee.model.oauth2.OAuth2Authorization
import io.tolgee.repository.oauth2.OAuth2AuthorizationRepository
import io.tolgee.security.oauth2.OAuth2Constants
import io.tolgee.service.security.UserAccountService
import java.time.Duration
import java.time.Instant
import java.util.Date

/**
 * Issues OAuth2 access tokens straight into the authorization store, so a test can exercise the resolver without
 * driving the whole authorization-code dance. [deleteAll] cleans up what a test issued.
 */
class OAuth2TestTokens(
  private val repository: OAuth2AuthorizationRepository,
  private val userAccountService: UserAccountService,
  private val keyGenerator: KeyGenerator,
) {
  private val issuedAuthorizationIds = mutableListOf<Long>()

  /**
   * @param projects project ids the token is bound to, or [OAuth2Constants.ALL_PROJECTS] for the not-narrowed sentinel.
   *   Any other value is stored as-is, so a test can plant an unparseable selection.
   */
  fun issue(
    subject: Long,
    scopes: List<String>,
    projects: Any = OAuth2Constants.ALL_PROJECTS,
    clientId: String = OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID,
    issuedAt: Instant = Instant.now(),
    expiresAt: Instant = issuedAt.plus(Duration.ofMinutes(30)),
  ): String {
    val token = keyGenerator.generate()
    val authorization =
      OAuth2Authorization().apply {
        userAccount = userAccountService.get(subject)
        this.clientId = clientId
        redirectUri = "https://example.org/callback"
        codeChallenge = "test-challenge"
        requestedScopeValues = scopes
        grantedScopeValues = scopes
        projectSelection = selectionOf(projects)
        accessTokenHash = keyGenerator.hash(token)
        accessTokenIssuedAt = Date.from(issuedAt)
        accessTokenExpiresAt = Date.from(expiresAt)
      }
    repository.save(authorization)
    issuedAuthorizationIds.add(authorization.id)
    return token
  }

  private fun selectionOf(projects: Any): String {
    if (projects is Collection<*>) return projects.joinToString(",")
    return projects.toString()
  }

  fun revoke(token: String) {
    repository.findByAccessTokenHash(keyGenerator.hash(token))?.let { repository.delete(it) }
  }

  fun deleteAll() {
    repository.deleteAllById(issuedAuthorizationIds)
    issuedAuthorizationIds.clear()
  }
}
