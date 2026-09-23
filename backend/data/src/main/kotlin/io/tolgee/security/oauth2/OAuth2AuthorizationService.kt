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

import io.tolgee.Metrics
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.KeyGenerator
import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.isTokenInvalidated
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.oauth2.OAuth2ClientDocumentCheck
import io.tolgee.model.oauth2.OAuth2Grant
import io.tolgee.model.oauth2.OAuth2SupersededRefreshToken
import io.tolgee.repository.oauth2.OAuth2ClientDocumentCheckRepository
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.repository.oauth2.OAuth2SupersededRefreshTokenRepository
import io.tolgee.security.OAUTH_ACCESS_TOKEN_PREFIX
import io.tolgee.security.OAUTH_REFRESH_TOKEN_PREFIX
import io.tolgee.security.oauth2.cimd.CimdMetadataFetcher
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * The OAuth 2.1 authorization-code grant with PKCE, for public clients only: every step of the protocol that touches
 * the grant store. HTTP shape (redirects, status codes, JSON) is the controller's job; this decides what is
 * valid and what gets issued.
 */
@Service
class OAuth2AuthorizationService(
  private val repository: OAuth2GrantRepository,
  private val supersededRefreshTokenRepository: OAuth2SupersededRefreshTokenRepository,
  private val userAccountService: UserAccountService,
  private val keyGenerator: KeyGenerator,
  private val currentDateProvider: CurrentDateProvider,
  private val properties: OAuth2ServerProperties,
  private val documentCheckRepository: OAuth2ClientDocumentCheckRepository,
  private val resources: OAuth2Resources,
  private val metrics: Metrics,
) : Logging {
  data class AuthorizeParams(
    val responseType: String?,
    val scope: String?,
    val state: String?,
    val codeChallenge: String?,
    val codeChallengeMethod: String?,
    val resource: String?,
  )

  data class ValidatedAuthorizeRequest(
    val scopes: List<String>,
    val codeChallenge: String,
    val audience: OAuth2Audience,
  )

  data class IssuedTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
    val scopes: List<String>,
    /** The project this grant is bound to, when it is bound to exactly one; null when a client must name one. */
    val projectId: Long?,
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
      params.codeChallenge?.takeIf { OAuth2Pkce.isValidCodeChallenge(it) }
        ?: throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "code_challenge is not a valid S256 challenge")
    return ValidatedAuthorizeRequest(scopes, challenge, resources.audienceFor(params.resource))
  }

  @Transactional
  fun startAuthorization(
    userId: Long,
    client: OAuth2Client,
    redirectUri: String,
    params: AuthorizeParams,
    projectHint: String?,
  ): OAuth2Grant {
    val validated = validateAuthorizeRequest(params)
    refuseIfTooManyDocumentBackedClients(userId, client)

    val grant =
      OAuth2Grant().apply {
        userAccount = userAccountService.get(userId)
        clientId = client.clientId
        this.redirectUri = redirectUri
        clientState = params.state
        codeChallenge = validated.codeChallenge
        requestedScopeValues = validated.scopes
        bindAudience(validated.audience)
        clientMetadataHash = client.metadataHash
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
    requestedAudience: OAuth2Audience?,
  ): IssuedTokens {
    if (code.isNullOrBlank()) throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "code is required")
    val verifier =
      codeVerifier?.takeIf { OAuth2Pkce.isValidCodeVerifier(it) }
        ?: throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "code_verifier is missing or malformed")
    val grant =
      repository.findAndLockByCodeHash(keyGenerator.hash(code)) ?: throw OAuth2Error(OAuth2Error.INVALID_GRANT)

    if (grant.codeUsedAt != null || grant.clientId != client.clientId || metadataDrifted(grant, client)) {
      repository.delete(grant)
      throw OAuth2Error(OAuth2Error.INVALID_GRANT)
    }
    if (isExpiredOrUnset(grant.codeExpiresAt)) {
      repository.delete(grant)
      throw OAuth2Error(OAuth2Error.INVALID_GRANT, "code expired")
    }
    refuseIfClientWithdrawn(grant)
    // RFC 9700 4.5.3.1: a mismatching redirect_uri or code_verifier is the authorization-code-injection signal, so
    // the code is spent rather than left redeemable for the rest of its validity across unlimited attempts.
    if (grant.redirectUri != redirectUri || !OAuth2Pkce.matchesChallenge(verifier, grant.codeChallenge)) {
      grant.codeUsedAt = currentDateProvider.date
      repository.save(grant)
      throw OAuth2Error(OAuth2Error.INVALID_GRANT)
    }
    // Blank, not just null: an empty collection binds "" here, which parses back into a token reaching no project
    // at all rather than into a refusal.
    if (grant.projectSelection.isNullOrBlank()) {
      throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "the consent did not bind a project set")
    }
    requireMatchingAudience(grant, requestedAudience)
    revokeAndFailIfUserInvalidated(grant)

    grant.codeUsedAt = currentDateProvider.date
    return issueTokens(grant)
  }

  @Transactional(noRollbackFor = [OAuth2Error::class])
  fun refresh(
    client: OAuth2Client,
    refreshToken: String?,
    requestedScope: String?,
    requestedAudience: OAuth2Audience?,
  ): IssuedTokens {
    if (refreshToken.isNullOrBlank()) throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "refresh_token is required")
    val hash = keyGenerator.hash(refreshToken.removePrefix(OAUTH_REFRESH_TOKEN_PREFIX))
    val grant = repository.findAndLockByRefreshTokenHash(hash) ?: revokeReplayedGrantAndFail(hash)
    // RFC 9700 §4.14.2: a refresh token surfacing under a client it was not issued to is the same compromise signal
    // as a code doing so, and exchangeCode kills the grant for it. Probing the other registered client must not be free.
    if (grant.clientId != client.clientId || metadataDrifted(grant, client)) {
      repository.delete(grant)
      throw OAuth2Error(OAuth2Error.INVALID_GRANT)
    }
    if (isExpiredOrUnset(grant.refreshTokenExpiresAt)) {
      repository.delete(grant)
      throw OAuth2Error(OAuth2Error.INVALID_GRANT, "refresh token expired")
    }
    refuseIfClientWithdrawn(grant)
    refuseIfDocumentUnreadForTooLong(grant, client)
    requireMatchingAudience(grant, requestedAudience)
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
    // Every refresh token the grant ever issued counts: a client that rotated twice and then logs out with an
    // older one would otherwise get the RFC 7009 answer for a dead token while its grant stayed live. A superseded
    // access token is overwritten in place and cannot be looked up at all.
    val grant =
      repository.findAndLockByAccessTokenHash(hash)
        ?: repository.findAndLockByRefreshTokenHash(hash)
        ?: repository.findAndLockByPreviousRefreshTokenHash(hash)
        ?: supersededRefreshTokenRepository.findAndLockByTokenHash(hash)?.grant
        ?: return
    if (grant.clientId != client.clientId) throw OAuth2Error(OAuth2Error.INVALID_GRANT)
    repository.delete(grant)
  }

  @Transactional
  fun recordDocumentRead(clientId: String) {
    val now = currentDateProvider.date
    val refreshBefore = Date(now.time - TimeUnit.DAYS.toMillis(properties.cimdVerificationMaxAgeDays) / 2)
    repository.markClientDocumentRead(clientId, now, refreshBefore)
  }

  /** The document no longer matches what its users agreed to, so those grants end. */
  @Transactional
  fun revokeDriftedFromDocument(
    clientId: String,
    currentHash: String?,
  ): Int {
    if (currentHash == null) return 0
    val revoked =
      repository.deleteDriftedFromDocument(
        clientId,
        currentHash,
        CimdMetadataFetcher.HASH_SCHEME_PREFIX + "%",
      )
    if (revoked > 0) {
      logger.warn(
        "Revoked {} OAuth2 grant(s) of client {}: its metadata document no longer matches the consented terms",
        revoked,
        clientId,
      )
    }
    return revoked
  }

  /**
   * Bounds how many document-backed clients one account adds to the background check's work list. See
   * `docs/oauth/README.md` for what that queue's length costs everybody else.
   */
  private fun refuseIfTooManyDocumentBackedClients(
    userId: Long,
    client: OAuth2Client,
  ) {
    if (!client.hasMetadataDocument) return
    val held = repository.countDocumentBackedClientsOfUser(userId, client.clientId, currentDateProvider.date)
    if (held < properties.cimdMaxClientsPerUser) return
    logger.warn("Refusing a new CIMD authorization for user {}: it already holds {} such clients", userId, held)
    throw OAuth2Error(
      OAuth2Error.ACCESS_DENIED,
      "this account already holds authorizations for too many clients that identify themselves with a document",
    )
  }

  fun clientIdsDueForCheck(limit: Int): List<String> =
    repository.findCimdClientIdsDueForCheck(
      currentDateProvider.date,
      dueBefore(),
      graceStart(),
      NEVER_CHECKED,
      PageRequest.of(0, limit),
    )

  fun clientsDueForCheckCount(): Long =
    repository.countCimdClientIdsDueForCheck(currentDateProvider.date, dueBefore(), graceStart(), NEVER_CHECKED)

  private fun dueBefore(): Date =
    Date(currentDateProvider.date.time - TimeUnit.MINUTES.toMillis(properties.cimdCheckIntervalMinutes))

  private fun graceStart(): Date =
    Date(currentDateProvider.date.time - TimeUnit.MINUTES.toMillis(properties.cimdWithdrawalGraceMinutes))

  @Transactional
  fun recordCheckAttempt(clientId: String) {
    val existing = documentCheckRepository.findByClientId(clientId)
    val row = existing ?: OAuth2ClientDocumentCheck().also { it.clientId = clientId }
    row.previousCheckedAt = existing?.checkedAt
    row.checkedAt = currentDateProvider.date
    documentCheckRepository.save(row)
  }

  @Transactional
  fun deleteCheckRowsWithoutGrants(): Int = documentCheckRepository.deleteWithoutGrants()

  /** A publisher's refusal, made durable on the grant rows so every instance reads it. */
  @Transactional
  fun recordClientWithdrawn(clientId: String) {
    val marked = repository.markClientWithdrawn(clientId, currentDateProvider.date)
    if (marked > 0) {
      logger.warn(
        "Marked {} OAuth2 grant(s) of client {} as withdrawn: its metadata document refuses",
        marked,
        clientId,
      )
    }
  }

  /**
   * A grant of a client that identifies itself with a metadata document lives on that document being readable.
   * Not having been read is forgiven until [OAuth2ServerProperties.cimdVerificationMaxAgeDays].
   */
  private fun refuseIfDocumentUnreadForTooLong(
    grant: OAuth2Grant,
    client: OAuth2Client,
  ) {
    if (!client.hasMetadataDocument) return
    val lastRead = grant.cimdVerifiedAt ?: grant.createdAt ?: return
    val maxAgeMs = TimeUnit.DAYS.toMillis(properties.cimdVerificationMaxAgeDays)
    if (currentDateProvider.date.time - lastRead.time <= maxAgeMs) return
    // Past the bound, the next question is whose doing that is: a client we kept *trying* to read is the
    // publisher's answer, while one we never got round to is our own downtime and must not end a grant.
    val lastAttempt = documentCheckRepository.findByClientId(grant.clientId)?.checkedAt
    if (lastAttempt == null || lastAttempt.time - lastRead.time <= maxAgeMs) {
      logger.warn(
        "Keeping grant {} of client {}: its document has not been read since {}, but neither has it been tried",
        grant.id,
        grant.clientId,
        lastRead,
      )
      return
    }
    logger.info(
      "Refusing a refresh of grant {}: its client's metadata document has not been readable since {}",
      grant.id,
      lastRead,
    )
    throw OAuth2Error(OAuth2Error.INVALID_GRANT, "the client's metadata document could not be read")
  }

  /**
   * The mark another instance may have written, read from the row and not from this instance's resolution cache.
   * The grant is kept, not deleted, so a document that answers again inside the grace window can clear the mark.
   */
  private fun refuseIfClientWithdrawn(grant: OAuth2Grant) {
    if (grant.clientWithdrawnAt == null) return
    throw OAuth2Error(OAuth2Error.INVALID_GRANT, "the client's metadata document is gone")
  }

  /**
   * The document resolves again. A mark younger than the grace window was a mis-deploy and is lifted; an older one
   * is the publisher retiring the client and stays.
   */
  @Transactional
  fun clearClientWithdrawn(clientId: String) {
    // This round's attempt is recorded after the work, so the row still holds the previous round's and
    // `previousCheckedAt` reaches two attempts back. Comparing against the latest one instead would make every
    // mark look "already read" on the very next round.
    val previousAttempt = documentCheckRepository.findByClientId(clientId)?.previousCheckedAt ?: NEVER_CHECKED
    val cleared = repository.clearRecentClientWithdrawn(clientId, graceStart(), previousAttempt)
    if (cleared > 0) {
      logger.info(
        "Lifted the withdrawal mark on {} OAuth2 grant(s) of client {}: its document answers again",
        cleared,
        clientId,
      )
    }
  }

  @Transactional
  fun revokeAllForUser(userId: Long): Int = repository.deleteAllByUserAccountId(userId)

  @Transactional
  fun deleteExpiredBefore(cutoff: Instant): Int = repository.deleteExpiredBefore(Date.from(cutoff))

  @Transactional
  fun pruneRefreshHistoryBeyondDepth(): Int =
    supersededRefreshTokenRepository.deleteBeyondNewestPerGrant(
      properties.refreshTokenHistoryGenerations,
      historyFloor(),
    )

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

  /** The CIMD client's document now describes different terms than the user consented to. */
  private fun metadataDrifted(
    grant: OAuth2Grant,
    client: OAuth2Client,
  ): Boolean {
    if (client.metadataHash == null) return false
    if (grant.clientMetadataHash == client.metadataHash) return false
    // A hash this build cannot reproduce proves nothing; reading it as drift would revoke every grant issued by
    // every earlier release on the first refresh after deploy.
    if (grant.clientMetadataHash?.startsWith(CimdMetadataFetcher.HASH_SCHEME_PREFIX) != true) return false
    logger.warn(
      "Revoking OAuth2 grant {} for client {}: its metadata document no longer matches the consented terms",
      grant.id,
      client.clientId,
    )
    return true
  }

  /** A client's config error, not a compromise signal. */
  private fun requireMatchingAudience(
    grant: OAuth2Grant,
    requested: OAuth2Audience?,
  ) {
    if (requested != null && requested != grant.boundAudience()) {
      throw OAuth2Error(OAuth2Error.INVALID_TARGET, "the grant was not authorized for this resource")
    }
  }

  /** RFC 9700 §4.14.2: the token is always refused, and the grant behind it may or may not survive. */
  private fun revokeReplayedGrantAndFail(hash: String): Nothing {
    revokeGrantIfReplayedOutsideGrace(hash)
    throw OAuth2Error(OAuth2Error.INVALID_GRANT)
  }

  /**
   * Deletes the grant the hash belonged to when a rotated-away refresh token is replayed after the grace window.
   * A replay inside the window, and a hash matching nothing at all, leave the grant as it is.
   */
  private fun revokeGrantIfReplayedOutsideGrace(hash: String) {
    val justRotated = repository.findAndLockByPreviousRefreshTokenHash(hash)
    if (justRotated != null) {
      if (isWithinRefreshGrace(justRotated.refreshTokenRotatedAt)) {
        recordGraceHit(justRotated.id, "the just-rotated refresh token was replayed within the grace window")
        return
      }
      logger.warn(
        "Revoking OAuth2 grant {}: its previous refresh token was replayed after the grace window",
        justRotated.id,
      )
      repository.delete(justRotated)
      return
    }
    val superseded = supersededRefreshTokenRepository.findAndLockByTokenHash(hash) ?: return
    if (isWithinRefreshGrace(superseded.supersededAt)) {
      recordGraceHit(superseded.grant.id, "a token superseded within the grace window was replayed")
      return
    }
    logger.warn("Revoking OAuth2 grant {}: a refresh token from an earlier rotation was replayed", superseded.grant.id)
    repository.delete(superseded.grant)
  }

  /** Keeping the grant is the whole point of the window, so the counter is the replay's only lasting trace. */
  private fun recordGraceHit(
    grantId: Long,
    reason: String,
  ) {
    metrics.oauth2RefreshGraceHitsCounter.increment()
    logger.warn("OAuth2 grant {} kept: {}", grantId, reason)
  }

  private fun isWithinRefreshGrace(stoppedBeingCurrentAt: Date?): Boolean {
    val stopped = stoppedBeingCurrentAt?.toInstant() ?: return false
    return !stopped.plusSeconds(properties.refreshTokenGraceSeconds).isBefore(currentDateProvider.date.toInstant())
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
    val demotedHash = grant.previousRefreshTokenHash
    val demotedAt = grant.refreshTokenRotatedAt
    grant.previousRefreshTokenHash = grant.refreshTokenHash
    grant.refreshTokenRotatedAt = currentDateProvider.date
    grant.refreshTokenHash = keyGenerator.hash(refreshToken)
    grant.refreshTokenExpiresAt = nowPlus(Duration.ofDays(properties.refreshTokenValidityDays))
    demoteToSupersededHistory(grant, demotedHash, demotedAt)
    repository.save(grant)
    return IssuedTokens(
      accessToken = OAUTH_ACCESS_TOKEN_PREFIX + accessToken,
      refreshToken = OAUTH_REFRESH_TOKEN_PREFIX + refreshToken,
      expiresInSeconds = validity.seconds,
      scopes = grant.issuedTokenScopeValues,
      projectId = grant.boundProjectIds()?.singleOrNull(),
    )
  }

  private fun demoteToSupersededHistory(
    grant: OAuth2Grant,
    demotedHash: String?,
    supersededAt: Date?,
  ) {
    if (demotedHash == null) return
    if (!makeRoomInHistory(grant)) {
      logger.warn(
        "OAuth2 grant {} is at its refresh-token history ceiling ({} rows), all of them younger than the retention " +
          "floor; not recording further rotations. A client rotating this fast is a runaway or a hostile one.",
        grant.id,
        MAX_HISTORY_ROWS_PER_GRANT,
      )
      return
    }
    // Touching grant.supersededRefreshTokens would initialise it: up to MAX_HISTORY_ROWS_PER_GRANT entities loaded
    // on a path that writes one row.
    supersededRefreshTokenRepository.save(
      OAuth2SupersededRefreshToken().apply {
        this.grant = grant
        tokenHash = demotedHash
        this.supersededAt = supersededAt ?: currentDateProvider.date
      },
    )
  }

  /**
   * Returns whether one more history row may be written, deleting the oldest row past the retention floor to get
   * there. The ceiling check has to guard the delete: called unguarded it would drop a row on every rotation.
   */
  private fun makeRoomInHistory(grant: OAuth2Grant): Boolean {
    if (!isAtHistoryCeiling(grant)) return true
    return supersededRefreshTokenRepository.deleteOldestPastFloor(grant.id, historyFloor()) > 0
  }

  private fun historyFloor(): Date =
    Date.from(currentDateProvider.date.toInstant().minus(Duration.ofDays(properties.refreshTokenHistoryMinDays)))

  private fun isAtHistoryCeiling(grant: OAuth2Grant): Boolean {
    val id = grant.id
    if (id == 0L) return false
    return supersededRefreshTokenRepository.countByGrantId(id) >= MAX_HISTORY_ROWS_PER_GRANT
  }

  private fun parseScopes(raw: String?): List<String> = OAuth2Scopes.splitScopeString(raw).distinct()

  private fun nowPlus(duration: Duration): Date = Date.from(currentDateProvider.date.toInstant().plus(duration))

  private fun isExpiredOrUnset(deadline: Date?): Boolean = deadline == null || !deadline.after(currentDateProvider.date)

  companion object {
    private val NEVER_CHECKED = Date(0)

    /** Matches the `client_state` column width; a longer state cannot be stored, so it must be refused up front. */
    const val MAX_STATE_LENGTH = 2000

    const val MAX_HISTORY_ROWS_PER_GRANT = 2000
  }
}
