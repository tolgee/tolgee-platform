package io.tolgee.security.oauth2

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.LockingProvider
import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * The cutoff computation is the only logic in the scheduled cleanup: a sign error would push it into the future and
 * delete authorizations whose refresh tokens are still live, logging active users out.
 */
class OAuth2GrantCleanupCutoffTest {
  @Test
  fun `deletes with a cutoff of exactly now minus the retention window`() {
    val now = Instant.parse("2026-08-07T00:00:00Z")
    val authorizationService = mock<OAuth2AuthorizationService>()
    val dateProvider = mock<CurrentDateProvider> { on { date } doReturn Date.from(now) }
    val properties = OAuth2ServerProperties().apply { grantRetentionDays = 7 }

    OAuth2GrantCleanup(authorizationService, properties, dateProvider, RunsImmediately()).cleanUpExpiredGrants()

    val captor = argumentCaptor<Instant>()
    verify(authorizationService).deleteExpiredBefore(captor.capture())
    captor.firstValue.assert.isEqualTo(now.minus(Duration.ofDays(7)))
  }

  private class RunsImmediately : LockingProvider {
    override fun getLock(name: String): Lock = ReentrantLock()

    override fun <T> withLocking(
      name: String,
      fn: () -> T,
    ): T = fn()

    override fun <T> withLockingIfFree(
      name: String,
      leaseTime: Duration,
      fn: () -> T,
    ): T = fn()
  }
}
