package io.tolgee.service.security

import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.SessionBookkeepingExecutorConfiguration
import io.tolgee.model.UserSession
import io.tolgee.model.enums.AuthAuditEventType
import io.tolgee.model.enums.UserSessionType
import io.tolgee.repository.UserSessionRepository
import io.tolgee.security.authentication.SessionEvictPublisher
import io.tolgee.security.authentication.UserSessionHotCache
import io.tolgee.util.GeoIpResolver
import io.tolgee.util.HibernateSequenceIdProvider
import io.tolgee.util.RequestIpProvider
import io.tolgee.util.RequestUserAgentProvider
import io.tolgee.util.runSentryCatching
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.Date

@Service
class UserSessionService(
  private val userSessionRepository: UserSessionRepository,
  private val authAuditService: AuthAuditService,
  private val currentDateProvider: CurrentDateProvider,
  private val requestIpProvider: RequestIpProvider,
  private val requestUserAgentProvider: RequestUserAgentProvider,
  private val geoIpResolver: GeoIpResolver,
  private val userSessionHotCache: UserSessionHotCache,
  private val sequenceIdProvider: HibernateSequenceIdProvider,
  private val sessionEvictPublisher: SessionEvictPublisher,
) {
  /**
   * Records the session a freshly emitted token belongs to. Joins the caller's transaction so a
   * rolled-back sign-up leaves no session behind.
   */
  @Transactional
  fun registerToken(
    deviceId: String,
    userAccountId: Long,
    type: UserSessionType,
    actingUserAccountId: Long?,
    expiresAt: Date,
    isRefresh: Boolean,
  ) {
    val now = currentDateProvider.date
    val ip = requestIpProvider.getClientIp()
    val location = geoIpResolver.resolve(ip)
    userSessionRepository.upsert(
      id = sequenceIdProvider.next(),
      now = now,
      deviceId = deviceId,
      userAccountId = userAccountId,
      type = insertType(type, isRefresh).name,
      ip = ip,
      userAgent = requestUserAgentProvider.getUserAgent(),
      expiresAt = expiresAt,
      lastRefreshedAt = refreshedAt(now, isRefresh),
      actingUserAccountId = actingUserAccountId,
      countryCode = location?.countryCode,
      country = location?.country,
      city = location?.city,
    )

    val session = userSessionRepository.findByDeviceId(deviceId)

    authAuditService.record(
      type = loginEventType(isRefresh, actingUserAccountId),
      userAccountId = userAccountId,
      actingUserAccountId = actingUserAccountId,
      deviceId = deviceId,
      targetId = session?.id,
      data = mutableMapOf("sessionType" to (session?.type ?: insertType(type, isRefresh)).name),
    )
  }

  /**
   * Creates the missing session row for a token that predates session tracking. Written
   * synchronously so a handler in the same request already finds the row - the authentication
   * filter runs outside any transaction, so this opens and commits one of its own. It must not
   * force a separate transaction: the row references the user account, and taking that foreign key
   * lock from a nested transaction would block against an outer one already holding the same row.
   */
  @Transactional
  fun backfillSession(
    deviceId: String,
    userAccountId: Long,
    expiresAt: Date,
    actingUserAccountId: Long?,
    ip: String?,
    userAgent: String?,
  ) {
    val location = geoIpResolver.resolve(ip)
    userSessionRepository.insertIfAbsent(
      id = sequenceIdProvider.next(),
      now = currentDateProvider.date,
      deviceId = deviceId,
      userAccountId = userAccountId,
      type = backfillType(actingUserAccountId).name,
      ip = ip,
      userAgent = userAgent,
      expiresAt = expiresAt,
      actingUserAccountId = actingUserAccountId,
      countryCode = location?.countryCode,
      country = location?.country,
      city = location?.city,
    )
  }

  fun findActive(
    userAccountId: Long,
    tokensValidNotBefore: Date?,
    pageable: Pageable,
  ): Page<UserSession> =
    userSessionRepository.findActive(
      userAccountId = userAccountId,
      now = currentDateProvider.date,
      // no watermark means nothing has ever been invalidated, so let every session through
      tokensValidNotBefore = tokensValidNotBefore ?: Date(0),
      impersonationType = UserSessionType.IMPERSONATION,
      pageable = pageable,
    )

  fun find(id: Long): UserSession? = userSessionRepository.findById(id).orElse(null)

  fun findByDeviceId(deviceId: String): UserSession? = userSessionRepository.findByDeviceId(deviceId)

  @Transactional
  fun revoke(
    session: UserSession,
    revokedById: Long,
  ) {
    revokeAll(listOf(session), revokedById)
  }

  /**
   * Revokes every session of the user except the one the caller is using. Deliberately sweeps
   * expired and otherwise-dead rows too, so they cannot linger or be resurrected by a backfill.
   */
  @Transactional
  fun revokeAllOthers(
    userAccountId: Long,
    currentDeviceId: String?,
    revokedById: Long,
  ): Int {
    val sessions =
      userSessionRepository.findRevocable(
        userAccountId = userAccountId,
        // a legacy token has no device of its own, so nothing is exempt from the sweep
        exceptDeviceId = currentDeviceId ?: "",
        impersonationType = UserSessionType.IMPERSONATION,
      )
    return revokeAll(sessions, revokedById)
  }

  @Transactional
  fun revokeCurrent(
    userAccountId: Long,
    deviceId: String?,
    revokedById: Long,
  ) {
    deviceId ?: return
    val session = userSessionRepository.findByDeviceId(deviceId) ?: return
    if (session.userAccountId != userAccountId) return
    revokeAll(listOf(session), revokedById)
  }

  @Async(SessionBookkeepingExecutorConfiguration.SESSION_BOOKKEEPING_EXECUTOR)
  @Transactional
  fun updateLastUsedAsync(
    deviceId: String,
    date: Date,
  ) {
    runSentryCatching {
      userSessionRepository.updateLastUsed(deviceId, date)
    }
  }

  private fun revokeAll(
    sessions: List<UserSession>,
    revokedById: Long,
  ): Int {
    val revocable = sessions.filter { it.revokedAt == null }
    if (revocable.isEmpty()) return 0

    val now = currentDateProvider.date
    userSessionRepository.revokeAllByIdIn(revocable.map { it.id }, now, revokedById)

    revocable.forEach {
      authAuditService.record(
        type = AuthAuditEventType.SESSION_REVOKED,
        userAccountId = it.userAccountId,
        deviceId = it.deviceId,
        targetId = it.id,
      )
    }

    scheduleEviction(revocable.map { it.deviceId })
    return revocable.size
  }

  /**
   * Evicting before the revocation commits would let a concurrent request re-cache the row as still
   * active, so the eviction waits for the commit.
   */
  private fun scheduleEviction(deviceIds: List<String>) {
    TransactionSynchronizationManager.registerSynchronization(
      object : TransactionSynchronization {
        override fun afterCommit() {
          deviceIds.forEach {
            userSessionHotCache.evict(it)
            sessionEvictPublisher.publish(it)
          }
        }
      },
    )
  }

  private fun insertType(
    type: UserSessionType,
    isRefresh: Boolean,
  ): UserSessionType {
    if (isRefresh) return UserSessionType.UNKNOWN
    return type
  }

  private fun backfillType(actingUserAccountId: Long?): UserSessionType {
    if (actingUserAccountId != null) return UserSessionType.IMPERSONATION
    return UserSessionType.UNKNOWN
  }

  private fun refreshedAt(
    now: Date,
    isRefresh: Boolean,
  ): Date? {
    if (isRefresh) return now
    return null
  }

  private fun loginEventType(
    isRefresh: Boolean,
    actingUserAccountId: Long?,
  ): AuthAuditEventType {
    if (isRefresh) return AuthAuditEventType.TOKEN_REFRESH
    if (actingUserAccountId != null) return AuthAuditEventType.IMPERSONATION
    return AuthAuditEventType.LOGIN
  }
}
