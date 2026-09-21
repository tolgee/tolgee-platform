package io.tolgee.unit.component

import io.tolgee.component.lockingProvider.RedissonLockingProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.redisson.api.RLock
import org.redisson.api.RedissonClient

class RedissonLockingProviderTest {
  private val lock = mock<RLock>()
  private val provider =
    RedissonLockingProvider(
      mock<RedissonClient> { on { getLock("name") } doReturn lock },
    )

  @AfterEach
  fun clearInterrupt() {
    Thread.interrupted()
  }

  @Test
  fun `unlocks after the body`() {
    provider.withLocking("name") { }
    verify(lock).unlock()
  }

  @Test
  fun `unlocks when the body interrupted the thread and keeps the interrupt`() {
    provider.withLocking("name") { Thread.currentThread().interrupt() }
    verify(lock).unlock()
    assertThat(Thread.currentThread().isInterrupted).isTrue
  }

  @Test
  fun `tolerates a lock that is no longer held`() {
    whenever(lock.unlock()).doThrow(IllegalMonitorStateException())
    provider.withLocking("name") { }
  }
}
