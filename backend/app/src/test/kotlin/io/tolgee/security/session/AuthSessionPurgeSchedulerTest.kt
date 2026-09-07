package io.tolgee.security.session

import io.tolgee.AbstractSpringTest
import io.tolgee.component.SchedulingManager
import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.model.AuthAuditEvent
import io.tolgee.model.UserSession
import io.tolgee.model.enums.AuthAuditEventType
import io.tolgee.model.enums.UserSessionType
import io.tolgee.repository.AuthAuditEventRepository
import io.tolgee.repository.UserSessionRepository
import io.tolgee.service.security.AuthSessionPurgeScheduler
import io.tolgee.testing.assert
import io.tolgee.util.addDays
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Duration
import java.util.Date
import java.util.UUID

/**
 * Deliberately not an [io.tolgee.testing.AuthorizedControllerTest]: its clock overrides refresh the
 * JWT, which would write fresh sessions and audit events in the middle of the purge assertions.
 */
@SpringBootTest
class AuthSessionPurgeSchedulerTest : AbstractSpringTest() {
  @Autowired
  lateinit var scheduler: AuthSessionPurgeScheduler

  @Autowired
  lateinit var userSessionRepository: UserSessionRepository

  @Autowired
  lateinit var authAuditEventRepository: AuthAuditEventRepository

  @Autowired
  lateinit var lockingProvider: io.tolgee.component.LockingProvider

  private var originalPurgeEnabled: Boolean = true

  @BeforeEach
  fun setup() {
    originalPurgeEnabled = tolgeeProperties.authentication.sessionAudit.purgeEnabled
    setForcedDate(Date())
  }

  @AfterEach
  fun cleanup() {
    tolgeeProperties.authentication.sessionAudit.purgeEnabled = originalPurgeEnabled
    clearForcedDate()
  }

  @Test
  fun `purges only what is past retention`() {
    val user = dbPopulator.createUserIfNotExists("purge-target@tolgee.io")
    val retention =
      tolgeeProperties.authentication.sessionAudit.expiredSessionRetentionDays
        .toInt()

    val oldExpired = seedSession(user.id, currentDateProvider.date.addDays(-retention - 1))
    val recentlyExpired = seedSession(user.id, currentDateProvider.date.addDays(-1))
    val revokedButUnexpired =
      seedSession(user.id, currentDateProvider.date.addDays(10), revoked = true)

    scheduler.purge()

    userSessionRepository.findByDeviceId(oldExpired).assert.isNull()
    userSessionRepository.findByDeviceId(recentlyExpired).assert.isNotNull
    userSessionRepository.findByDeviceId(revokedButUnexpired).assert.isNotNull
  }

  @Test
  fun `purges audit events past retention across more than one batch`() {
    val retention =
      tolgeeProperties.authentication.sessionAudit.auditEventRetentionDays
        .toInt()
    val old = currentDateProvider.date.addDays(-retention - 1)
    val count = AuthSessionPurgeScheduler.BATCH_SIZE + 1

    executeInNewTransaction {
      repeat(count) {
        val event =
          AuthAuditEvent().apply {
            type = AuthAuditEventType.LOGIN
            attemptedUsername = "purge-me"
          }
        authAuditEventRepository.save(event)
        entityManager
          .createNativeQuery("update auth_audit_event set created_at = :date where id = :id")
          .setParameter("date", old)
          .setParameter("id", event.id)
          .executeUpdate()
      }
    }

    scheduler.purge()

    authAuditEventRepository
      .findAll()
      .filter { it.attemptedUsername == "purge-me" }
      .assert
      .isEmpty()
  }

  @Test
  fun `registers the scheduled task only when purging is enabled`() {
    val period = Duration.ofMillis(tolgeeProperties.authentication.sessionAudit.purgeDelayMs)
    val schedulingManagerMock = mock<SchedulingManager>()

    schedulerWith(schedulingManagerMock, purgeEnabled = true).schedulePurge()
    verify(schedulingManagerMock, times(1)).scheduleWithFixedDelay(any(), eq(period))

    clearInvocations(schedulingManagerMock)
    schedulerWith(schedulingManagerMock, purgeEnabled = false).schedulePurge()
    verify(schedulingManagerMock, never()).scheduleWithFixedDelay(any(), eq(period))
  }

  private fun schedulerWith(
    schedulingManager: SchedulingManager,
    purgeEnabled: Boolean,
  ): AuthSessionPurgeScheduler {
    val properties = AuthenticationProperties()
    properties.sessionAudit.purgeEnabled = purgeEnabled
    properties.sessionAudit.purgeDelayMs = tolgeeProperties.authentication.sessionAudit.purgeDelayMs
    return AuthSessionPurgeScheduler(
      userSessionRepository,
      authAuditEventRepository,
      properties,
      currentDateProvider,
      lockingProvider,
      platformTransactionManager,
      schedulingManager,
    )
  }

  private fun seedSession(
    userAccountId: Long,
    expiresAt: Date,
    revoked: Boolean = false,
  ): String {
    val deviceId = UUID.randomUUID().toString()
    executeInNewTransaction {
      userSessionRepository.save(
        UserSession().apply {
          this.deviceId = deviceId
          this.userAccountId = userAccountId
          this.type = UserSessionType.LOGIN_NATIVE
          this.expiresAt = expiresAt
          if (revoked) {
            this.revokedAt = currentDateProvider.date
            this.revokedById = userAccountId
          }
        },
      )
    }
    return deviceId
  }
}
