package io.tolgee.ee.fixtures

import io.tolgee.development.testDataBuilder.newOAuth2Grant
import io.tolgee.model.UserAccount
import io.tolgee.model.oauth2.OAuth2Grant
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.security.oauth2.OAuth2Constants
import java.util.Date
import java.util.UUID

/**
 * The listing query requires a non-null `projectSelection`, a non-null `refreshTokenHash` and a
 * future `refreshTokenExpiresAt` all at once; a grant missing any one of them silently does not
 * list. Call inside `executeInNewTransaction`.
 */
fun seedConnectedGrant(
  grantRepository: OAuth2GrantRepository,
  user: UserAccount,
  now: Date,
  clientId: String = OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID,
  projectIds: List<Long>? = null,
  consented: Boolean = true,
  refreshExpired: Boolean = false,
): OAuth2Grant {
  val grant = newOAuth2Grant(user, clientId = clientId)
  if (consented) {
    grant.bindProjects(projectIds)
    grant.issuedTokenScopeValues = grant.requestedScopeValues
  }
  grant.refreshTokenHash = "hash-" + UUID.randomUUID()
  grant.refreshTokenExpiresAt = if (refreshExpired) Date(now.time - 1000) else Date(now.time + 86_400_000)
  grant.accessTokenIssuedAt = now
  return grantRepository.save(grant)
}
