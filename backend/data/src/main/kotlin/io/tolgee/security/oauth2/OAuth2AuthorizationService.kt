package io.tolgee.security.oauth2

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.KeyGenerator
import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import io.tolgee.dtos.cacheable.isTokenInvalidated
import io.tolgee.model.oauth2.OAuth2Authorization
import io.tolgee.repository.oauth2.OAuth2AuthorizationRepository
import io.tolgee.service.security.UserAccountService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Date

/**
 * The OAuth 2.1 authorization-code grant with PKCE, for public clients only: every step of the protocol that touches
 * the authorization store. HTTP shape (redirects, status codes, JSON) is the controller's job; this decides what is
 * valid and what gets issued.
 */
@Service
class OAuth2AuthorizationService(
  private val repository: OAuth2AuthorizationRepository,
  private val userAccountService: UserAccountService,
  private val keyGenerator: KeyGenerator,
  private val currentDateProvider: CurrentDateProvider,
  private val properties: OAuth2ServerProperties,
) {
  data class AuthorizeParams(
    val responseType: String?,
    val scope: String?,
    val state: String?,
    val codeChallenge: String?,
    val codeChallengeMethod: String?,
    val projectHint: String?,
  )

  data class IssuedTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
    val scopes: List<String>,
  )

  fun findByConsentState(consentState: String): OAuth2Authorization? = repository.findByConsentState(consentState)

  @Transactional
  fun save(authorization: OAuth2Authorization): OAuth2Authorization = repository.save(authorization)

  /**
   * Validates the redirectable part of an authorize request (client and redirect URI are the caller's responsibility,
   * since errors here are sent back to that URI) and records the pending authorization the consent screen works on.
   */
  @Transactional
  fun startAuthorization(
    userId: Long,
    client: OAuth2Client,
    redirectUri: String,
    params: AuthorizeParams,
  ): OAuth2Authorization {
    if (params.responseType != "code") throw OAuth2Error(OAuth2Error.UNSUPPORTED_RESPONSE_TYPE)
    val scopes = parseScopes(params.scope)
    if (scopes.isEmpty() || scopes.any { !client.allowsScope(it) }) throw OAuth2Error(OAuth2Error.INVALID_SCOPE)
    if (params.codeChallenge.isNullOrBlank()) {
      throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "code_challenge is required")
    }
    if (params.codeChallengeMethod != "S256") {
      throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "code_challenge_method must be S256")
    }

    val authorization =
      OAuth2Authorization().apply {
        userAccount = userAccountService.get(userId)
        clientId = client.clientId
        this.redirectUri = redirectUri
        clientState = params.state
        codeChallenge = params.codeChallenge
        requestedScopeValues = scopes
        projectHint = params.projectHint?.toLongOrNull()
        consentState = keyGenerator.generate()
      }
    return repository.save(authorization)
  }

  /** Records the user's approval and issues the single-use code; the scopes kept are the requested ones the user approved. */
  @Transactional(noRollbackFor = [OAuth2Error::class])
  fun approveConsent(
    authorization: OAuth2Authorization,
    approvedScopes: List<String>,
  ): String {
    if (authorization.codeHash != null) throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "consent already given")
    val requested = authorization.requestedScopeValues
    if (approvedScopes.any { it !in requested }) {
      repository.delete(authorization)
      throw OAuth2Error(OAuth2Error.INVALID_SCOPE, "approved scope was not requested")
    }
    val code = keyGenerator.generate()
    authorization.grantedScopeValues = requested.filter { it in approvedScopes }
    authorization.codeHash = keyGenerator.hash(code)
    authorization.codeExpiresAt = plus(Duration.ofSeconds(properties.authorizationCodeValiditySeconds))
    repository.save(authorization)
    return code
  }

  @Transactional
  fun denyConsent(authorization: OAuth2Authorization) {
    repository.delete(authorization)
  }

  @Transactional(noRollbackFor = [OAuth2Error::class])
  fun exchangeCode(
    client: OAuth2Client,
    code: String?,
    redirectUri: String?,
    codeVerifier: String?,
  ): IssuedTokens {
    if (code.isNullOrBlank()) throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "code is required")
    if (codeVerifier.isNullOrBlank()) throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "code_verifier is required")
    val authorization =
      repository.findByCodeHash(keyGenerator.hash(code)) ?: throw OAuth2Error(OAuth2Error.INVALID_GRANT)

    // A code presented twice, or by a client it was not issued to, is treated as stolen: everything it produced dies.
    if (authorization.codeUsedAt != null || authorization.clientId != client.clientId) {
      repository.delete(authorization)
      throw OAuth2Error(OAuth2Error.INVALID_GRANT)
    }
    if (isPast(authorization.codeExpiresAt)) {
      repository.delete(authorization)
      throw OAuth2Error(OAuth2Error.INVALID_GRANT, "code expired")
    }
    if (authorization.redirectUri != redirectUri) throw OAuth2Error(OAuth2Error.INVALID_GRANT)
    if (s256(codeVerifier) != authorization.codeChallenge) throw OAuth2Error(OAuth2Error.INVALID_GRANT)
    // `project` on the authorize request is the client's own choice; a token is only ever bound to what the consent
    // screen actually showed and the user picked.
    if (authorization.projectSelection == null) {
      throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "the consent did not bind a project set")
    }

    authorization.codeUsedAt = currentDateProvider.date
    return issueTokens(authorization)
  }

  @Transactional(noRollbackFor = [OAuth2Error::class])
  fun refresh(
    client: OAuth2Client,
    refreshToken: String?,
    requestedScope: String?,
  ): IssuedTokens {
    if (refreshToken.isNullOrBlank()) throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "refresh_token is required")
    val authorization =
      repository.findByRefreshTokenHash(keyGenerator.hash(refreshToken)) ?: throw OAuth2Error(OAuth2Error.INVALID_GRANT)
    if (authorization.clientId != client.clientId) throw OAuth2Error(OAuth2Error.INVALID_GRANT)
    if (isPast(authorization.refreshTokenExpiresAt)) {
      repository.delete(authorization)
      throw OAuth2Error(OAuth2Error.INVALID_GRANT, "refresh token expired")
    }
    // A refresh-minted access token carries a fresh issue time, so the resolver's tokensValidNotBefore check would let
    // it through; the grant itself has to predate the invalidation to be refreshable.
    val user = userAccountService.findDto(authorization.userAccount.id)
    if (user == null || user.isTokenInvalidated(authorization.createdAt?.toInstant())) {
      repository.delete(authorization)
      throw OAuth2Error(OAuth2Error.INVALID_GRANT)
    }
    requestedScope?.let { narrowScopes(authorization, parseScopes(it)) }
    return issueTokens(authorization)
  }

  /** Deletes ALL of the user's authorizations (logout-everywhere, password change, account deletion); returns the row count. */
  @Transactional
  fun revokeAllForUser(userId: Long): Int = repository.deleteAllByUserAccountId(userId)

  @Transactional
  fun deleteExpiredBefore(cutoff: Instant): Int = repository.deleteExpiredBefore(Date.from(cutoff))

  private fun narrowScopes(
    authorization: OAuth2Authorization,
    scopes: List<String>,
  ) {
    val granted = authorization.grantedScopeValues
    if (scopes.isEmpty() || scopes.any { it !in granted }) throw OAuth2Error(OAuth2Error.INVALID_SCOPE)
    authorization.grantedScopeValues = granted.filter { it in scopes }
  }

  /** Rotation: the grant carries one access and one refresh token, so issuing replaces whatever it had. */
  private fun issueTokens(authorization: OAuth2Authorization): IssuedTokens {
    val accessToken = keyGenerator.generate()
    val refreshToken = keyGenerator.generate()
    val validity = Duration.ofMinutes(properties.accessTokenValidityMinutes)
    authorization.accessTokenHash = keyGenerator.hash(accessToken)
    authorization.accessTokenIssuedAt = currentDateProvider.date
    authorization.accessTokenExpiresAt = plus(validity)
    authorization.refreshTokenHash = keyGenerator.hash(refreshToken)
    authorization.refreshTokenExpiresAt = plus(Duration.ofDays(properties.refreshTokenValidityDays))
    repository.save(authorization)
    return IssuedTokens(accessToken, refreshToken, validity.seconds, authorization.grantedScopeValues)
  }

  private fun parseScopes(raw: String?): List<String> =
    raw
      .orEmpty()
      .split(" ")
      .filter { it.isNotBlank() }
      .distinct()

  private fun plus(duration: Duration): Date = Date.from(currentDateProvider.date.toInstant().plus(duration))

  private fun isPast(date: Date?): Boolean = date == null || !date.after(currentDateProvider.date)

  private fun s256(verifier: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
  }
}
