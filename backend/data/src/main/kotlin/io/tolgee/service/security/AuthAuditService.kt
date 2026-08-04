package io.tolgee.service.security

import io.tolgee.model.AuthAuditEvent
import io.tolgee.model.enums.AuthAuditEventType
import io.tolgee.repository.AuthAuditEventRepository
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.util.RequestIpProvider
import io.tolgee.util.RequestUserAgentProvider
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class AuthAuditService(
  private val authAuditEventRepository: AuthAuditEventRepository,
  private val requestIpProvider: RequestIpProvider,
  private val requestUserAgentProvider: RequestUserAgentProvider,
  @Lazy
  private val authenticationFacade: AuthenticationFacade,
) {
  @Transactional
  fun record(
    type: AuthAuditEventType,
    userAccountId: Long? = null,
    attemptedUsername: String? = null,
    actingUserAccountId: Long? = null,
    deviceId: String? = null,
    targetId: Long? = null,
    data: MutableMap<String, Any?>? = null,
  ): AuthAuditEvent = save(type, userAccountId, attemptedUsername, actingUserAccountId, deviceId, targetId, data)

  /**
   * Records in a separate transaction, so the event survives the exception (and the rollback) that
   * the failure it describes is about to cause.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun recordIndependently(
    type: AuthAuditEventType,
    userAccountId: Long? = null,
    attemptedUsername: String? = null,
    actingUserAccountId: Long? = null,
    deviceId: String? = null,
    targetId: Long? = null,
    data: MutableMap<String, Any?>? = null,
  ): AuthAuditEvent = save(type, userAccountId, attemptedUsername, actingUserAccountId, deviceId, targetId, data)

  private fun save(
    type: AuthAuditEventType,
    userAccountId: Long?,
    attemptedUsername: String?,
    actingUserAccountId: Long?,
    deviceId: String?,
    targetId: Long?,
    data: MutableMap<String, Any?>?,
  ): AuthAuditEvent {
    val event =
      AuthAuditEvent().apply {
        this.type = type
        this.userAccountId = userAccountId
        this.attemptedUsername = attemptedUsername?.take(MAX_ATTEMPTED_USERNAME_LENGTH)
        this.actingUserAccountId = actingUserAccountId ?: currentActingUserId()
        this.deviceId = deviceId ?: currentDeviceId()
        this.targetId = targetId
        this.ip = requestIpProvider.getClientIp()
        this.userAgent = requestUserAgentProvider.getUserAgent()
        this.data = data
      }
    return authAuditEventRepository.save(event)
  }

  private fun currentActingUserId(): Long? {
    if (!authenticationFacade.isAuthenticated) return null
    return authenticationFacade.actingUser?.id
  }

  private fun currentDeviceId(): String? {
    if (!authenticationFacade.isAuthenticated) return null
    return authenticationFacade.deviceId
  }

  companion object {
    /**
     * The attempted username comes straight from an unauthenticated request body, so it is not
     * bounded by anything but this.
     */
    const val MAX_ATTEMPTED_USERNAME_LENGTH = 255
  }
}
