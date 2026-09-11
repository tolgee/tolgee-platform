package io.tolgee.security.authentication

import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthExpiredException
import io.tolgee.service.security.UserSessionService
import io.tolgee.util.Logging
import io.tolgee.util.RequestIpProvider
import io.tolgee.util.RequestUserAgentProvider
import io.tolgee.util.logger
import org.springframework.context.annotation.Lazy
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.util.Date

/**
 * Decides whether the session behind a token is still allowed to authenticate. Sits on every
 * authenticated request, so it answers from memory and only reads the database when it has nothing
 * cached for the device.
 */
@Component
class UserSessionAccessManager(
  private val jdbcTemplate: JdbcTemplate,
  @Lazy
  private val userSessionService: UserSessionService,
  private val userSessionHotCache: UserSessionHotCache,
  private val currentDateProvider: CurrentDateProvider,
  private val authenticationProperties: AuthenticationProperties,
  private val requestIpProvider: RequestIpProvider,
  private val requestUserAgentProvider: RequestUserAgentProvider,
) : Logging {
  fun checkSessionAndTrackUsage(
    deviceId: String,
    userAccountId: Long,
    expiresAt: Date,
    actingUserAccountId: Long?,
  ) {
    val cached = userSessionHotCache.get(deviceId)
    if (cached != null) {
      if (cached.revoked) {
        throw AuthExpiredException(Message.EXPIRED_JWT_TOKEN)
      }
      trackUsage(deviceId, cached)
      return
    }

    val session = readSession(deviceId)

    if (session == null) {
      backfill(deviceId, userAccountId, expiresAt, actingUserAccountId)
      userSessionHotCache.put(
        deviceId,
        UserSessionHotCache.Entry(revoked = false, lastUsedWrittenAt = currentDateProvider.date.time),
      )
      return
    }

    if (session.userAccountId != userAccountId) {
      throw AuthExpiredException(Message.EXPIRED_JWT_TOKEN)
    }

    if (session.revoked) {
      userSessionHotCache.put(deviceId, UserSessionHotCache.Entry(revoked = true, lastUsedWrittenAt = 0))
      throw AuthExpiredException(Message.EXPIRED_JWT_TOKEN)
    }

    val entry =
      UserSessionHotCache.Entry(
        revoked = false,
        // a session that was never used yet is as recent as its creation, so the first write can
        // wait for the interval instead of landing on every fresh login
        lastUsedWrittenAt = session.lastUsedAt ?: session.createdAt,
      )
    userSessionHotCache.put(deviceId, entry)
    trackUsage(deviceId, entry)
  }

  private fun backfill(
    deviceId: String,
    userAccountId: Long,
    expiresAt: Date,
    actingUserAccountId: Long?,
  ) {
    try {
      userSessionService.backfillSession(
        deviceId = deviceId,
        userAccountId = userAccountId,
        expiresAt = expiresAt,
        actingUserAccountId = actingUserAccountId,
        ip = requestIpProvider.getTrustedClientIp(),
        userAgent = requestUserAgentProvider.getUserAgent(),
      )
    } catch (e: Exception) {
      // Outside the transaction on purpose: the row this collides with belongs to a registration
      // that has not committed, so the insert waits, hits the lock timeout and aborts its own
      // transaction. That transaction is about to create the very row this wanted, and the caller's
      // work must not be affected by it - which it would be if the catch were any deeper.
      logger.debug("Session $deviceId is being written elsewhere; skipping backfill", e)
    }
  }

  /**
   * Deliberately not a JPA query. This runs on the authentication filter, inside whatever
   * transaction the request has already opened, and Hibernate flushes a persistence context before
   * querying it - flushing a caller's half-built entities on the way past changed the outcome of an
   * unrelated import request. Reading through JDBC keeps the caller's context untouched, and avoids
   * hydrating an entity on a hot path for four columns.
   */
  private fun readSession(deviceId: String): SessionRow? =
    jdbcTemplate
      .query(
        "select user_account_id, revoked_at, last_used_at, created_at from user_session where device_id = ?",
        { rs, _ ->
          SessionRow(
            userAccountId = rs.getLong("user_account_id"),
            revoked = rs.getTimestamp("revoked_at") != null,
            lastUsedAt = rs.getTimestamp("last_used_at")?.time,
            createdAt = rs.getTimestamp("created_at")?.time ?: 0,
          )
        },
        deviceId,
      ).firstOrNull()

  private data class SessionRow(
    val userAccountId: Long,
    val revoked: Boolean,
    val lastUsedAt: Long?,
    val createdAt: Long,
  )

  /**
   * The stamp is moved before the write is dispatched, so a burst of concurrent requests produces
   * one write rather than one per request.
   */
  private fun trackUsage(
    deviceId: String,
    entry: UserSessionHotCache.Entry,
  ) {
    val now = currentDateProvider.date
    val interval = authenticationProperties.sessionAudit.sessionLastUsedUpdateIntervalMs
    if (now.time - entry.lastUsedWrittenAt <= interval) {
      return
    }

    entry.lastUsedWrittenAt = now.time
    userSessionService.updateLastUsedAsync(deviceId, now)
  }
}
