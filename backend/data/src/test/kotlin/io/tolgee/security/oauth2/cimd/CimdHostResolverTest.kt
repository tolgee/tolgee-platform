package io.tolgee.security.oauth2.cimd

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.tolgee.Metrics
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.testing.assert
import io.tolgee.util.UrlSecurity
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.net.InetAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CimdHostResolverTest {
  @Test
  fun `one host may only strand its share of the resolver threads`() {
    val release = CountDownLatch(1)
    val parked = CountDownLatch(CimdHostResolver.MAX_RESOLUTIONS_PER_HOST)
    val urlSecurity =
      mock<UrlSecurity> {
        on { validateUrlAndResolve(any(), any()) } doAnswer {
          parked.countDown()
          release.await()
          listOf(loopback())
        }
      }
    val resolver = CimdHostResolver(urlSecurity, InternalProperties(), Metrics(SimpleMeterRegistry()))
    val pool = Executors.newFixedThreadPool(CimdHostResolver.MAX_RESOLUTIONS_PER_HOST)
    repeat(CimdHostResolver.MAX_RESOLUTIONS_PER_HOST) { i ->
      pool.submit { resolver.resolve("https://one-host.invalid:${8000 + i}/client-$i", CimdFetchLane.REQUEST) }
    }
    parked.await(10, TimeUnit.SECONDS).assert.isTrue()

    try {
      assertThatThrownBy { resolver.resolve("https://one-host.invalid/another", CimdFetchLane.REQUEST) }
        .isInstanceOf(CimdNoCapacityException::class.java)
      // Another host still gets in: the cap is per host, not a global one this host just spent.
      resolver.resolve("https://other-host.invalid/client", CimdFetchLane.REQUEST)
      verify(urlSecurity).validateUrlAndResolve(eq("https://other-host.invalid/client"), any())
    } finally {
      release.countDown()
      pool.shutdown()
      pool.awaitTermination(10, TimeUnit.SECONDS)
    }
  }

  @Test
  fun `a host whose lookup ran past the deadline is not given another thread straight away`() {
    val urlSecurity =
      mock<UrlSecurity> {
        on { validateUrlAndResolve(any(), any()) } doAnswer {
          Thread.sleep(10_000)
          listOf(loopback())
        }
      }
    val metrics = Metrics(SimpleMeterRegistry())
    val resolver = CimdHostResolver(urlSecurity, InternalProperties(), metrics)

    resolver.resolve("https://stuck.invalid/client", CimdFetchLane.REQUEST).assert.isNull()
    resolver.resolve("https://stuck.invalid/other", CimdFetchLane.REQUEST).assert.isNull()

    verify(urlSecurity, times(1)).validateUrlAndResolve(any(), any())
    // Both the lookup that ran past the deadline and the one refused because of it: a host whose lookups keep
    // hanging is exactly what an operator alerting on this counter needs to see.
    metrics.oauth2CimdCapacityRefusalsCounter
      .count()
      .assert
      .isEqualTo(2.0)
  }

  @Test
  fun `a host stuck on the request lane is still read for the background check`() {
    val urlSecurity =
      mock<UrlSecurity> {
        on { validateUrlAndResolve(any(), any()) } doAnswer {
          Thread.sleep(10_000)
          listOf(loopback())
        }
      }
    val resolver = CimdHostResolver(urlSecurity, InternalProperties(), Metrics(SimpleMeterRegistry()))

    resolver.resolve("https://stuck.invalid/client", CimdFetchLane.REQUEST).assert.isNull()
    resolver.resolve("https://stuck.invalid/client", CimdFetchLane.GRANT_CHECK).assert.isNull()

    verify(urlSecurity, times(2)).validateUrlAndResolve(any(), any())
  }

  /**
   * The request lane's resolver pool is smaller than the number of fetches the budget admits, so a rejection by it
   * happens in ordinary traffic. If the rejection kept the host's slot, the counter would ratchet up until the
   * host was refused for good and a legitimate publisher went offline.
   */
  @Test
  fun `a rejection by the resolver pool gives the host slot back`() {
    val release = CountDownLatch(1)
    val parked = CountDownLatch(CimdHostResolver.MAX_CONCURRENT_RESOLUTIONS)
    val urlSecurity =
      mock<UrlSecurity> {
        on { validateUrlAndResolve(any(), any()) } doAnswer {
          parked.countDown()
          release.await()
          listOf(loopback())
        }
      }
    val resolver = CimdHostResolver(urlSecurity, InternalProperties(), Metrics(SimpleMeterRegistry()))
    val hosts = CimdHostResolver.MAX_CONCURRENT_RESOLUTIONS / CimdHostResolver.MAX_RESOLUTIONS_PER_HOST
    val pool = Executors.newFixedThreadPool(CimdHostResolver.MAX_CONCURRENT_RESOLUTIONS)
    repeat(hosts) { host ->
      repeat(CimdHostResolver.MAX_RESOLUTIONS_PER_HOST) { i ->
        pool.submit { resolver.resolve("https://parked-$host.invalid/client-$i", CimdFetchLane.REQUEST) }
      }
    }
    parked.await(20, TimeUnit.SECONDS).assert.isTrue()

    try {
      // Every one of these is refused because no resolver thread is free, not because of the victim's own share.
      repeat(CimdHostResolver.MAX_RESOLUTIONS_PER_HOST + 1) {
        assertThatThrownBy {
          resolver.resolve(
            VICTIM,
            CimdFetchLane.REQUEST,
          )
        }.isInstanceOf(CimdNoCapacityException::class.java)
      }
    } finally {
      release.countDown()
      pool.shutdown()
      pool.awaitTermination(20, TimeUnit.SECONDS)
    }

    resolveUntilAThreadIsFree(resolver)

    verify(urlSecurity).validateUrlAndResolve(eq(VICTIM), any())
  }

  /** The parked threads go back to the pool a moment after they are released, not before. */
  private fun resolveUntilAThreadIsFree(resolver: CimdHostResolver) {
    val giveUpAt = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
    while (System.nanoTime() < giveUpAt) {
      val refused = runCatching { resolver.resolve(VICTIM, CimdFetchLane.REQUEST) }.exceptionOrNull()
      if (refused == null) return
      if (refused !is CimdNoCapacityException) throw refused
      Thread.sleep(50)
    }
  }

  private fun loopback(): InetAddress = InetAddress.getByName("127.0.0.1")

  companion object {
    private const val VICTIM = "https://victim.invalid/client"
  }
}
