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
import io.tolgee.model.oauth2.OAuth2Grant
import io.tolgee.repository.oauth2.OAuth2GrantRepository
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
 * the grant store. HTTP shape (redirects, status codes, JSON) is the controller's job; this decides what is
 * valid and what gets issued.
 */
@Service
class OAuth2AuthorizationService(
  private val repository: OAuth2GrantRepository,
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

  fun validateAuthorizeRequest(params: AuthorizeParams): ValidatedAuthorizeRequest {
    if (params.responseType == null) throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "response_type is required")
    if (params.responseType != "code") throw OAuth2Error(OAuth2Error.UNSUPPORTED_RESPONSE_TYPE)
    val scopes = parseScopes(params.scope)
    if (scopes.isEmpty() || scopes.any { !OAuth2Scopes.isSupported(it) }) throw OAuth2Error(OAuth2Error.INVALID_SCOPE)
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
  ): OAuth2Grant {
    val (scopes, challenge) = validateAuthorizeRequest(params)

    val grant =
      OAuth2Grant().apply {
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
    return repository.save(grant)
  }

  /**
   * A plain read for the consent screen. The decision itself re-reads under a lock in [approveConsent] and
   * [denyConsent].
   */
  fun findOwnPendingByConsentState(
    consentState: String,
    userId: Long,
  ): OAuth2Grant = requireOwnPending(repository.findByConsentState(consentState), userId)

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
  fun approveConsent(
    consentState: String,
    userId: Long,
    approvedScopes: List<String>,
    projectIds: Collection<Long>?,
  ): ResolvedConsent {
    val grant = lockOwnPendingGrant(consentState, userId)
    val redirectUri = grant.redirectUri
    val clientState = grant.clientState

    val requested = grant.requestedScopeValues
    if (approvedScopes.any { it !in requested }) {
      repository.delete(grant)
      val error = OAuth2Error(OAuth2Error.INVALID_SCOPE, "approved scope was not requested")
      return ResolvedConsent.Refused(error, redirectUri, clientState)
    }
    val granted = requested.filter { it in approvedScopes }
    return ResolvedConsent.Granted(bindConsentAndMintCode(grant, granted, projectIds), redirectUri, clientState)
  }

  @Transactional
  fun denyConsent(
    consentState: String,
    userId: Long,
  ): ResolvedConsent {
    val grant = lockOwnPendingGrant(consentState, userId)
    val refused = ResolvedConsent.Refused(OAuth2Error(OAuth2Error.ACCESS_DENIED), grant.redirectUri, grant.clientState)
    repository.delete(grant)
    return refused
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
    val grant =
      repository.findAndLockByCodeHash(keyGenerator.hash(code)) ?: throw OAuth2Error(OAuth2Error.INVALID_GRANT)

    if (grant.codeUsedAt != null || grant.clientId != client.clientId) {
      repository.delete(grant)
      throw OAuth2Error(OAuth2Error.INVALID_GRANT)
    }
    if (isExpiredOrUnset(grant.codeExpiresAt)) {
      repository.delete(grant)
      throw OAuth2Error(OAuth2Error.INVALID_GRANT, "code expired")
    }
    // RFC 9700 4.5.3.1: a mismatching redirect_uri or code_verifier is the authorization-code-injection signal, so
    // the code is spent rather than left redeemable for the rest of its validity across unlimited attempts.
    if (grant.redirectUri != redirectUri || !constantTimeEquals(s256(verifier), grant.codeChallenge)) {
      grant.codeUsedAt = currentDateProvider.date
      repository.save(grant)
      throw OAuth2Error(OAuth2Error.INVALID_GRANT)
    }
    // Blank, not just null: an empty collection binds "" here, which parses back into a token reaching no project
    // at all rather than into a refusal.
    if (grant.projectSelection.isNullOrBlank()) {
      throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "the consent did not bind a project set")
    }
    revokeAndFailIfUserInvalidated(grant)

    grant.codeUsedAt = currentDateProvider.date
    return issueTokens(grant)
  }

  @Transactional(noRollbackFor = [OAuth2Error::class])
  fun refresh(
    client: OAuth2Client,
    refreshToken: String?,
    requestedScope: String?,
  ): IssuedTokens {
    if (refreshToken.isNullOrBlank()) throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "refresh_token is required")
    val hash = keyGenerator.hash(refreshToken.removePrefix(OAUTH_REFRESH_TOKEN_PREFIX))
    val grant = repository.findAndLockByRefreshTokenHash(hash) ?: revokeSupersededAndFail(hash)
    // RFC 9700 §4.14.2: a refresh token surfacing under a client it was not issued to is the same compromise signal
    // as a code doing so, and exchangeCode kills the grant for it. Probing the other registered client must not be free.
    if (grant.clientId != client.clientId) {
      repository.delete(grant)
      throw OAuth2Error(OAuth2Error.INVALID_GRANT)
    }
    if (isExpiredOrUnset(grant.refreshTokenExpiresAt)) {
      repository.delete(grant)
      throw OAuth2Error(OAuth2Error.INVALID_GRANT, "refresh token expired")
    }
    revokeAndFailIfUserInvalidated(grant)
    grant.issuedTokenScopeValues = narrowedScopes(grant, requestedScope)
    return issueTokens(grant)
  }

  /**
   * RFC 7009 §2.1: the token may be either kind, and the server "verifies whether the token was issued to the client
   * making the revocation request. If this validation fails, the request is refused". §2.2's identical-answer rule
   * covers a token that matched nothing, so an unknown token still returns quietly.
   */
  @Transactional
  fun revokeToken(
    client: OAuth2Client,
    token: String,
  ) {
    val hash = keyGenerator.hash(token.removePrefix(OAUTH_ACCESS_TOKEN_PREFIX).removePrefix(OAUTH_REFRESH_TOKEN_PREFIX))
    // The superseded *refresh* token counts too: a client that rotated and then logs out with the token it replaced
    // would otherwise be told the grant is dead while it stays live for the whole refresh window. A superseded
    // access token is overwritten in place and cannot be looked up.
    val grant =
      repository.findAndLockByAccessTokenHash(hash)
        ?: repository.findAndLockByRefreshTokenHash(hash)
        ?: repository.findAndLockByPreviousRefreshTokenHash(hash)
        ?: return
    if (grant.clientId != client.clientId) throw OAuth2Error(OAuth2Error.INVALID_GRANT)
    repository.delete(grant)
  }

  @Transactional
  fun revokeAllForUser(userId: Long): Int = repository.deleteAllByUserAccountId(userId)

  @Transactional
  fun deleteExpiredBefore(cutoff: Instant): Int = repository.deleteExpiredBefore(Date.from(cutoff))

  @Transactional
  fun deleteExpiredPendingConsents(): Int = repository.deleteExpiredPendingConsents(currentDateProvider.date)

  private fun lockOwnPendingGrant(
    consentState: String,
    userId: Long,
  ): OAuth2Grant = requireOwnPending(repository.findAndLockByConsentState(consentState), userId)

  /**
   * Every refusal is the same NotFoundException: a state that is not yours must be indistinguishable from one
   * that never existed.
   */
  private fun requireOwnPending(
    grant: OAuth2Grant?,
    userId: Long,
  ): OAuth2Grant {
    val pending =
      grant?.takeIf { !isExpiredOrUnset(it.consentExpiresAt) }
        ?: throw NotFoundException(Message.OAUTH_UNKNOWN_STATE)
    if (pending.userAccount.id != userId) throw NotFoundException(Message.OAUTH_UNKNOWN_STATE)
    return pending
  }

  private fun bindConsentAndMintCode(
    grant: OAuth2Grant,
    granted: List<String>,
    projectIds: Collection<Long>?,
  ): String {
    val code = keyGenerator.generate()
    grant.maxGrantedScopeValues = granted
    grant.issuedTokenScopeValues = granted
    grant.bindProjects(projectIds)
    grant.consentState = null
    grant.consentExpiresAt = null
    grant.codeHash = keyGenerator.hash(code)
    grant.codeExpiresAt = nowPlus(Duration.ofSeconds(properties.authorizationCodeValiditySeconds))
    repository.save(grant)
    return code
  }

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
   * it through; the check is against the grant's own creation time instead, so a grant older than the cutoff can no
   * longer produce tokens.
   */
  private fun revokeAndFailIfUserInvalidated(grant: OAuth2Grant) {
    val user = userAccountService.findDto(grant.userAccount.id)
    if (user == null || user.isTokenInvalidated(grant.createdAt?.toInstant())) {
      repository.delete(grant)
      throw OAuth2Error(OAuth2Error.INVALID_GRANT)
    }
  }

  /**
   * RFC 6749 §6: a refresh may ask for less than was granted, never more. The narrowing applies to the token being
   * issued — [OAuth2Grant.maxGrantedScopeValues] stays the ceiling, so a later refresh can ask for the full set
   * back.
   */
  private fun narrowedScopes(
    grant: OAuth2Grant,
    requestedScope: String?,
  ): List<String> {
    val granted = grant.maxGrantedScopeValues
    if (requestedScope == null) return granted
    val requested = parseScopes(requestedScope)
    if (requested.isEmpty() || requested.any { it !in granted }) throw OAuth2Error(OAuth2Error.INVALID_SCOPE)
    return granted.filter { it in requested }
  }

  private fun issueTokens(grant: OAuth2Grant): IssuedTokens {
    val accessToken = keyGenerator.generate()
    val refreshToken = keyGenerator.generate()
    val validity = Duration.ofMinutes(properties.accessTokenValidityMinutes)
    grant.accessTokenHash = keyGenerator.hash(accessToken)
    grant.accessTokenIssuedAt = currentDateProvider.date
    grant.accessTokenExpiresAt = nowPlus(validity)
    grant.previousRefreshTokenHash = grant.refreshTokenHash
    grant.refreshTokenHash = keyGenerator.hash(refreshToken)
    grant.refreshTokenExpiresAt = nowPlus(Duration.ofDays(properties.refreshTokenValidityDays))
    repository.save(grant)
    return IssuedTokens(
      accessToken = OAUTH_ACCESS_TOKEN_PREFIX + accessToken,
      refreshToken = OAUTH_REFRESH_TOKEN_PREFIX + refreshToken,
      expiresInSeconds = validity.seconds,
      scopes = grant.issuedTokenScopeValues,
    )
  }

  private fun parseScopes(raw: String?): List<String> = OAuth2Scopes.splitScopeString(raw).distinct()

  private fun nowPlus(duration: Duration): Date = Date.from(currentDateProvider.date.toInstant().plus(duration))

  private fun isExpiredOrUnset(deadline: Date?): Boolean = deadline == null || !deadline.after(currentDateProvider.date)

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
