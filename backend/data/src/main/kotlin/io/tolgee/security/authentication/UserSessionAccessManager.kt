package io.tolgee.security.authentication

import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthExpiredException
import io.tolgee.repository.UserSessionRepository
import io.tolgee.service.security.UserSessionService
import io.tolgee.util.RequestIpProvider
import io.tolgee.util.RequestUserAgentProvider
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.util.Date

/**
 * Decides whether the session behind a token is still allowed to authenticate. Sits on every
 * authenticated request, so it answers from memory and only reads the database when it has nothing
 * cached for the device.
 */
@Component
class UserSessionAccessManager(
  private val userSessionRepository: UserSessionRepository,
  @Lazy
  private val userSessionService: UserSessionService,
  private val userSessionHotCache: UserSessionHotCache,
  private val currentDateProvider: CurrentDateProvider,
  private val authenticationProperties: AuthenticationProperties,
  private val requestIpProvider: RequestIpProvider,
  private val requestUserAgentProvider: RequestUserAgentProvider,
) {
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

    val session = userSessionRepository.findByDeviceId(deviceId)

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

    if (session.revokedAt != null) {
      userSessionHotCache.put(deviceId, UserSessionHotCache.Entry(revoked = true, lastUsedWrittenAt = 0))
      throw AuthExpiredException(Message.EXPIRED_JWT_TOKEN)
    }

    val entry =
      UserSessionHotCache.Entry(
        revoked = false,
        // a session that was never used yet is as recent as its creation, so the first write can
        // wait for the interval instead of landing on every fresh login
        lastUsedWrittenAt = session.lastUsedAt?.time ?: session.createdAt?.time ?: 0,
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
    userSessionService.backfillSession(
      deviceId = deviceId,
      userAccountId = userAccountId,
      expiresAt = expiresAt,
      actingUserAccountId = actingUserAccountId,
      ip = requestIpProvider.getClientIp(),
      userAgent = requestUserAgentProvider.getUserAgent(),
    )
  }

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
