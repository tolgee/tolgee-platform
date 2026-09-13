package io.tolgee.fixtures

import io.tolgee.component.KeyGenerator
import io.tolgee.development.testDataBuilder.newOAuth2Grant
import io.tolgee.model.oauth2.OAuth2Grant
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.security.OAUTH_ACCESS_TOKEN_PREFIX
import io.tolgee.security.oauth2.OAuth2Constants
import io.tolgee.service.security.UserAccountService
import java.time.Duration
import java.time.Instant
import java.util.Date

/**
 * Issues OAuth2 access tokens straight into the grant store, so a test can exercise the resolver without
 * driving the whole authorization-code dance. [deleteAll] cleans up what a test issued.
 */
class OAuth2TestTokens(
  private val repository: OAuth2GrantRepository,
  private val userAccountService: UserAccountService,
  private val keyGenerator: KeyGenerator,
) {
  private val issuedGrantIds = mutableListOf<Long>()

  fun issue(
    subject: Long,
    scopes: List<String>,
    projectIds: Collection<Long>?,
    clientId: String = OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID,
    issuedAt: Instant = Instant.now(),
    expiresAt: Instant = issuedAt.plus(Duration.ofMinutes(30)),
  ): String {
    val token = keyGenerator.generate()
    val grant =
      newOAuth2Grant(userAccountService.get(subject), clientId, scopes).apply {
        maxGrantedScopeValues = scopes
        issuedTokenScopeValues = scopes
        bindProjects(projectIds)
        accessTokenHash = keyGenerator.hash(token)
        accessTokenIssuedAt = Date.from(issuedAt)
        accessTokenExpiresAt = Date.from(expiresAt)
      }
    repository.save(grant)
    issuedGrantIds.add(grant.id)
    return OAUTH_ACCESS_TOKEN_PREFIX + token
  }

  fun corruptProjectSelection(
    token: String,
    raw: String,
  ) {
    val grant = findByToken(token) ?: error("no grant for the given token")
    grant.projectSelection = raw
    repository.save(grant)
  }

  fun revoke(token: String) {
    findByToken(token)?.let { repository.delete(it) }
  }

  fun deleteAll() {
    repository.deleteAllById(issuedGrantIds)
    issuedGrantIds.clear()
  }

  private fun findByToken(token: String): OAuth2Grant? =
    repository.findByAccessTokenHash(keyGenerator.hash(token.removePrefix(OAUTH_ACCESS_TOKEN_PREFIX)))
}
