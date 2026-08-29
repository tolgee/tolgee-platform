/**
 * Copyright (C) 2026 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.security.oauth2

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.KeyGenerator
import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.isTokenInvalidated
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.oauth2.OAuth2Authorization
import io.tolgee.repository.oauth2.OAuth2AuthorizationRepository
import io.tolgee.security.OAUTH_ACCESS_TOKEN_PREFIX
import io.tolgee.security.OAUTH_REFRESH_TOKEN_PREFIX
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
  )

  data class ValidatedAuthorizeRequest(
    val scopes: List<String>,
    val codeChallenge: String,
  )

  data class IssuedTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
    val scopes: List<String>,
  )

  /** A plain read for the consent screen. The decision itself re-reads under a lock in [resolveConsent]. */
  fun findPendingByConsentState(consentState: String): OAuth2Authorization? =
    repository.findByConsentState(consentState)?.takeIf { !isExpired(it.consentExpiresAt) }

  /** Where to send the browser once the decision is recorded. */
  sealed class ResolvedConsent {
    abstract val redirectUri: String
    abstract val clientState: String?

    data class Granted(
      val code: String,
      override val redirectUri: String,
      override val clientState: String?,
    ) : ResolvedConsent()

    data class Refused(
      val error: OAuth2Error,
      override val redirectUri: String,
      override val clientState: String?,
    ) : ResolvedConsent()
  }

  /**
   * The lock must not be released between the re-check and the write: two submissions of one state would then both
   * mint a code, and the first client's code would already be unredeemable when it arrived.
   */
  @Transactional
  fun resolveConsent(
    consentState: String,
    userId: Long,
    approvedScopes: List<String>,
    projectIds: Collection<Long>?,
  ): ResolvedConsent {
    val authorization =
      repository.findAndLockByConsentState(consentState)?.takeIf { !isExpired(it.consentExpiresAt) }
        ?: throw NotFoundException(Message.OAUTH_UNKNOWN_STATE)
    if (authorization.userAccount.id != userId) throw NotFoundException(Message.OAUTH_UNKNOWN_STATE)

    // Read before approving: an unrequested scope deletes the row, and the redirect still has to be built.
    val redirectUri = authorization.redirectUri
    val clientState = authorization.clientState
    if (approvedScopes.isEmpty()) {
      repository.delete(authorization)
      return ResolvedConsent.Refused(OAuth2Error(OAuth2Error.ACCESS_DENIED), redirectUri, clientState)
    }
    return try {
      ResolvedConsent.Granted(approveConsent(authorization, approvedScopes, projectIds), redirectUri, clientState)
    } catch (e: OAuth2Error) {
      ResolvedConsent.Refused(e, redirectUri, clientState)
    }
  }

  /**
   * Everything about an authorize request that can be judged without knowing who the user is, so the authorization
   * endpoint can reject a malformed request before anyone logs in.
   */
  fun validateAuthorizeRequest(params: AuthorizeParams): ValidatedAuthorizeRequest {
    if (params.responseType == null) throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "response_type is required")
    if (params.responseType != "code") throw OAuth2Error(OAuth2Error.UNSUPPORTED_RESPONSE_TYPE)
    val scopes = parseScopes(params.scope)
    if (scopes.isEmpty() || scopes.any { !OAuth2Scopes.isSupported(it) }) throw OAuth2Error(OAuth2Error.INVALID_SCOPE)
    // The GET must reject everything the consent POST will: past the column width the grant cannot be stored, and by
    // then the browser has left the only endpoint that can answer the client machine-readably.
    if ((params.state?.length ?: 0) > MAX_STATE_LENGTH) {
      throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "state is too long")
    }
    if (params.codeChallengeMethod != "S256") {
      throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "code_challenge_method must be S256")
    }
    val challenge =
      params.codeChallenge?.takeIf { isValidCodeChallenge(it) }
        ?: throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "code_challenge is not a valid S256 challenge")
    return ValidatedAuthorizeRequest(scopes, challenge)
  }

  @Transactional
  fun startAuthorization(
    userId: Long,
    client: OAuth2Client,
    redirectUri: String,
    params: AuthorizeParams,
    projectHint: String?,
  ): OAuth2Authorization {
    val (scopes, challenge) = validateAuthorizeRequest(params)

    val authorization =
      OAuth2Authorization().apply {
        userAccount = userAccountService.get(userId)
        clientId = client.clientId
        this.redirectUri = redirectUri
        clientState = params.state
        codeChallenge = challenge
        requestedScopeValues = scopes
        this.projectHint = projectHint?.toLongOrNull()
        consentState = keyGenerator.generate()
        consentExpiresAt = nowPlus(Duration.ofSeconds(properties.consentValiditySeconds))
      }
    return repository.save(authorization)
  }

  private fun approveConsent(
    authorization: OAuth2Authorization,
    approvedScopes: List<String>,
    projectIds: Collection<Long>?,
  ): String {
    val requested = authorization.requestedScopeValues
    if (approvedScopes.any { it !in requested }) {
      repository.delete(authorization)
      throw OAuth2Error(OAuth2Error.INVALID_SCOPE, "approved scope was not requested")
    }
    val code = keyGenerator.generate()
    val granted = requested.filter { it in approvedScopes }
    authorization.grantedScopeValues = granted
    authorization.activeScopeValues = granted
    authorization.bindProjects(projectIds)
    authorization.consentState = null
    authorization.consentExpiresAt = null
    authorization.codeHash = keyGenerator.hash(code)
    authorization.codeExpiresAt = nowPlus(Duration.ofSeconds(properties.authorizationCodeValiditySeconds))
    repository.save(authorization)
    return code
  }

  @Transactional(noRollbackFor = [OAuth2Error::class])
  fun exchangeCode(
    client: OAuth2Client,
    code: String?,
    redirectUri: String?,
    codeVerifier: String?,
  ): IssuedTokens {
    if (code.isNullOrBlank()) throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "code is required")
    val verifier =
      codeVerifier?.takeIf { isValidCodeVerifier(it) }
        ?: throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "code_verifier is missing or malformed")
    // Locked, not merely read: two concurrent redemptions of the same code must not both observe it unspent and each
    // walk away believing it holds the grant's only token pair.
    val authorization =
      repository.findAndLockByCodeHash(keyGenerator.hash(code)) ?: throw OAuth2Error(OAuth2Error.INVALID_GRANT)

    // A code presented twice, or by a client it was not issued to, is treated as stolen: everything it produced dies.
    if (authorization.codeUsedAt != null || authorization.clientId != client.clientId) {
      repository.delete(authorization)
      throw OAuth2Error(OAuth2Error.INVALID_GRANT)
    }
    if (isExpired(authorization.codeExpiresAt)) {
      repository.delete(authorization)
      throw OAuth2Error(OAuth2Error.INVALID_GRANT, "code expired")
    }
    if (authorization.redirectUri != redirectUri) throw OAuth2Error(OAuth2Error.INVALID_GRANT)
    if (!constantTimeEquals(s256(verifier), authorization.codeChallenge)) {
      throw OAuth2Error(OAuth2Error.INVALID_GRANT)
    }
    // `project` on the authorize request is the client's own choice; a token is only ever bound to what the consent
    // screen actually showed and the user picked.
    if (authorization.projectSelection == null) {
      throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "the consent did not bind a project set")
    }
    requireLiveGrant(authorization)

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
    val hash = keyGenerator.hash(refreshToken.removePrefix(OAUTH_REFRESH_TOKEN_PREFIX))
    val authorization = repository.findAndLockByRefreshTokenHash(hash) ?: revokeSupersededAndFail(hash)
    if (authorization.clientId != client.clientId) throw OAuth2Error(OAuth2Error.INVALID_GRANT)
    if (isExpired(authorization.refreshTokenExpiresAt)) {
      repository.delete(authorization)
      throw OAuth2Error(OAuth2Error.INVALID_GRANT, "refresh token expired")
    }
    requireLiveGrant(authorization)
    authorization.activeScopeValues = narrowedScopes(authorization, requestedScope)
    return issueTokens(authorization)
  }

  @Transactional
  fun revokeAllForUser(userId: Long): Int = repository.deleteAllByUserAccountId(userId)

  @Transactional
  fun deleteExpiredBefore(cutoff: Instant): Int = repository.deleteExpiredBefore(Date.from(cutoff))

  @Transactional
  fun deleteExpiredPendingConsents(): Int = repository.deleteExpiredPendingConsents(currentDateProvider.date)

  /**
   * RFC 9700 §4.14.2: the token the current one replaced turning up means it was captured — the legitimate client
   * and the attacker cannot both hold the current one — so the grant dies rather than the replay merely failing.
   */
  private fun revokeSupersededAndFail(hash: String): Nothing {
    repository.findAndLockByPreviousRefreshTokenHash(hash)?.let { repository.delete(it) }
    throw OAuth2Error(OAuth2Error.INVALID_GRANT)
  }

  /**
   * A refresh-minted access token carries a fresh issue time, so the resolver's `tokensValidNotBefore` check would let
   * it through; the grant itself has to predate the invalidation to keep producing tokens.
   */
  private fun requireLiveGrant(authorization: OAuth2Authorization) {
    val user = userAccountService.findDto(authorization.userAccount.id)
    if (user == null || user.isTokenInvalidated(authorization.createdAt?.toInstant())) {
      repository.delete(authorization)
      throw OAuth2Error(OAuth2Error.INVALID_GRANT)
    }
  }

  /**
   * RFC 6749 §6: a refresh may ask for less than was granted, never more. The narrowing applies to the token being
   * issued — [OAuth2Authorization.grantedScopeValues] stays the ceiling, so a later refresh can ask for the full set
   * back.
   */
  private fun narrowedScopes(
    authorization: OAuth2Authorization,
    requestedScope: String?,
  ): List<String> {
    val granted = authorization.grantedScopeValues
    if (requestedScope == null) return granted
    val requested = parseScopes(requestedScope)
    if (requested.isEmpty() || requested.any { it !in granted }) throw OAuth2Error(OAuth2Error.INVALID_SCOPE)
    return granted.filter { it in requested }
  }

  private fun issueTokens(authorization: OAuth2Authorization): IssuedTokens {
    val accessToken = keyGenerator.generate()
    val refreshToken = keyGenerator.generate()
    val validity = Duration.ofMinutes(properties.accessTokenValidityMinutes)
    authorization.accessTokenHash = keyGenerator.hash(accessToken)
    authorization.accessTokenIssuedAt = currentDateProvider.date
    authorization.accessTokenExpiresAt = nowPlus(validity)
    authorization.previousRefreshTokenHash = authorization.refreshTokenHash
    authorization.refreshTokenHash = keyGenerator.hash(refreshToken)
    authorization.refreshTokenExpiresAt = nowPlus(Duration.ofDays(properties.refreshTokenValidityDays))
    repository.save(authorization)
    return IssuedTokens(
      accessToken = OAUTH_ACCESS_TOKEN_PREFIX + accessToken,
      refreshToken = OAUTH_REFRESH_TOKEN_PREFIX + refreshToken,
      expiresInSeconds = validity.seconds,
      scopes = authorization.activeScopeValues,
    )
  }

  private fun parseScopes(raw: String?): List<String> = OAuth2Constants.splitScopeString(raw).distinct()

  private fun nowPlus(duration: Duration): Date = Date.from(currentDateProvider.date.toInstant().plus(duration))

  /** Fail-closed: a deadline that was never recorded counts as expired. */
  private fun isExpired(deadline: Date?): Boolean = deadline == null || !deadline.after(currentDateProvider.date)

  private fun s256(verifier: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
  }

  private fun constantTimeEquals(
    a: String,
    b: String,
  ): Boolean = MessageDigest.isEqual(a.toByteArray(Charsets.US_ASCII), b.toByteArray(Charsets.US_ASCII))

  // RFC 7636 §4.1: 43-128 characters of unreserved ASCII.
  private fun isValidCodeVerifier(verifier: String): Boolean =
    verifier.length in 43..128 && verifier.all { it in PKCE_UNRESERVED }

  // RFC 7636 §4.2: an S256 challenge is the base64url-without-padding SHA-256 digest, i.e. exactly 43 such characters.
  private fun isValidCodeChallenge(challenge: String): Boolean =
    challenge.length == 43 && challenge.all { it in PKCE_UNRESERVED }

  companion object {
    /** Matches the `client_state` column width; a longer state cannot be stored, so it must be refused up front. */
    const val MAX_STATE_LENGTH = 2000

    private val PKCE_UNRESERVED =
      (('A'..'Z') + ('a'..'z') + ('0'..'9') + listOf('-', '.', '_', '~')).toSet()
  }
}
