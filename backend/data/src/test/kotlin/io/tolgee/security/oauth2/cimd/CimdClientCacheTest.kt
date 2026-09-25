package io.tolgee.security.oauth2.cimd

import com.github.benmanes.caffeine.cache.Ticker
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.tolgee.Metrics
import io.tolgee.security.oauth2.OAuth2Client
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class CimdClientCacheTest {
  private val metrics = Metrics(SimpleMeterRegistry())

  @Test
  fun `callers with no budget are refused rather than parked behind the one doing the fetch`() {
    val release = CountDownLatch(1)
    val started = CountDownLatch(1)
    val fetcher =
      mock<CimdMetadataFetcher> {
        on { fetchAndValidate(any(), any()) } doAnswer {
          started.countDown()
          release.await()
          resolved(URL)
        }
      }
    val cache = newCache(fetcher, budget = CimdFetchBudget(maxConcurrent = 1, maxConcurrentPerOrigin = 1))
    val pool = Executors.newSingleThreadExecutor()
    pool.submit { cache.get(URL) }
    started.await(10, TimeUnit.SECONDS).assert.isTrue()

    // Would block until release.countDown() if the budget sat inside the loader.
    cache.get(URL).assert.isNull()
    // The refusal is otherwise indistinguishable from an unreachable host, and this is the lane an authorize
    // burst fills, so the counter is what an operator alerts on.
    metrics.oauth2CimdCapacityRefusalsCounter
      .count()
      .assert
      .isEqualTo(1.0)

    release.countDown()
    pool.shutdown()
    pool.awaitTermination(10, TimeUnit.SECONDS)
  }

  @Test
  fun `a budget refusal is not cached, so one burst cannot suppress a legitimate client`() {
    val other = "https://app.example.com/other-client"
    val release = CountDownLatch(1)
    val started = CountDownLatch(1)
    val fetcher =
      mock<CimdMetadataFetcher> {
        on { fetchAndValidate(eq(URL), any()) } doAnswer {
          started.countDown()
          release.await()
          resolved(URL)
        }
        on { fetchAndValidate(eq(other), any()) } doReturn resolved(other)
      }
    val cache = newCache(fetcher, budget = CimdFetchBudget(maxConcurrent = 1, maxConcurrentPerOrigin = 1))
    val pool = Executors.newSingleThreadExecutor()
    pool.submit { cache.get(URL) }
    started.await(10, TimeUnit.SECONDS).assert.isTrue()

    cache.get(other).assert.isNull()

    release.countDown()
    pool.shutdown()
    pool.awaitTermination(10, TimeUnit.SECONDS)
    cache.get(other).assert.isNotNull()
  }

  @Test
  fun `a resolved client is fetched once and then served from the cache`() {
    val fetcher = mock<CimdMetadataFetcher> { on { fetchAndValidate(any(), any()) } doReturn resolved(URL) }
    val cache = newCache(fetcher)

    repeat(5) { cache.get(URL).assert.isNotNull() }

    verify(fetcher, times(1)).fetchAndValidate(eq(URL), any())
  }

  @Test
  fun `N concurrent cold requests for one client_id trigger exactly one fetch`() {
    val calls = AtomicInteger(0)
    val release = CountDownLatch(1)
    val fetcher =
      mock<CimdMetadataFetcher> {
        on { fetchAndValidate(any(), any()) } doAnswer {
          calls.incrementAndGet()
          release.await()
          resolved(URL)
        }
      }
    val cache = newCache(fetcher)

    val threads = 16
    val started = CountDownLatch(threads)
    val pool = Executors.newFixedThreadPool(threads)
    val results =
      (1..threads).map {
        pool.submit {
          started.countDown()
          cache.get(URL)
        }
      }
    started.await()
    release.countDown()
    results.forEach { it.get() }
    pool.shutdown()

    calls.get().assert.isEqualTo(1)
  }

  @Test
  fun `a fat client costs more of the cache than a small one, so the bound is on heap and not on count`() {
    val fat =
      mock<CimdMetadataFetcher> {
        on { fetchAndValidate(any(), any()) } doAnswer {
          val url: String = it.getArgument(0)
          CimdResolution.Resolved(
            CimdClient(
              OAuth2Client(url, url, (1..20).map { i -> "$url/cb-$i-" + "p".repeat(1000) }, verified = false),
              clientOrigin = url,
            ),
          )
        }
      }
    val cache = newCache(fat, approximateMaxEntries = 10)

    repeat(200) { cache.get("https://churn-$it.example/client") }

    cache.estimatedSize().assert.isLessThan(10)
  }

  @Test
  fun `the cache is bounded at its cap under attacker-chosen key churn`() {
    val fetcher =
      mock<CimdMetadataFetcher> {
        on { fetchAndValidate(any(), any()) } doAnswer { resolved(it.getArgument(0)) }
      }
    val cache = newCache(fetcher, approximateMaxEntries = 10)

    repeat(1000) { cache.get("https://churn-$it.example/client") }

    cache.estimatedSize().assert.isLessThan(50)
  }

  @Test
  fun `a document the host says is gone is remembered far longer than one that could not be fetched`() {
    val clock = AtomicLong(0)
    val refused = "https://refused.example/client"
    val unreachable = "https://unreachable.example/client"
    val fetcher =
      mock<CimdMetadataFetcher> {
        on { fetchAndValidate(eq(refused), any()) } doReturn CimdResolution.Withdrawn
        on { fetchAndValidate(eq(unreachable), any()) } doReturn CimdResolution.Unavailable
      }
    val cache = newCache(fetcher, ticker = clock::get)

    cache.get(refused)
    cache.get(unreachable)
    clock.set(seconds(30))
    cache.get(refused)
    cache.get(unreachable)

    verify(fetcher, times(1)).fetchAndValidate(eq(refused), any())
    verify(fetcher, times(2)).fetchAndValidate(eq(unreachable), any())
  }

  @Test
  fun `an invalidated entry is refetched on the next lookup`() {
    val fetcher = mock<CimdMetadataFetcher> { on { fetchAndValidate(any(), any()) } doReturn resolved(URL) }
    val cache = newCache(fetcher)

    cache.get(URL)
    cache.invalidate(URL)
    cache.get(URL)

    verify(fetcher, times(2)).fetchAndValidate(eq(URL), any())
  }

  @Test
  fun `an unresolved client_id is cached as a negative rather than refetched every time`() {
    val fetcher =
      mock<CimdMetadataFetcher> { on { fetchAndValidate(any(), any()) } doReturn CimdResolution.Unavailable }
    val cache = newCache(fetcher)

    repeat(5) { cache.get(URL).assert.isNull() }

    verify(fetcher, times(1)).fetchAndValidate(eq(URL), any())
  }

  @Test
  fun `a resolved client is refetched only once its positive TTL has passed, and reads do not extend it`() {
    val clock = AtomicLong(0)
    val fetcher = mock<CimdMetadataFetcher> { on { fetchAndValidate(any(), any()) } doReturn resolved(URL) }
    val cache = newCache(fetcher, positiveTtlSeconds = 300, unavailableTtlSeconds = 60, ticker = clock::get)

    cache.get(URL)
    clock.set(seconds(299))
    cache.get(URL)
    verify(fetcher, times(1)).fetchAndValidate(eq(URL), any())

    clock.set(seconds(301))
    cache.get(URL)
    verify(fetcher, times(2)).fetchAndValidate(eq(URL), any())
  }

  @Test
  fun `an unreachable client_id is retried on the shorter TTL, not held for the positive one`() {
    val clock = AtomicLong(0)
    val fetcher =
      mock<CimdMetadataFetcher> { on { fetchAndValidate(any(), any()) } doReturn CimdResolution.Unavailable }
    val cache = newCache(fetcher, positiveTtlSeconds = 300, unavailableTtlSeconds = 60, ticker = clock::get)

    cache.get(URL)
    clock.set(seconds(59))
    cache.get(URL)
    verify(fetcher, times(1)).fetchAndValidate(eq(URL), any())

    clock.set(seconds(61))
    cache.get(URL)
    verify(fetcher, times(2)).fetchAndValidate(eq(URL), any())
  }

  @Test
  fun `what the check reads does not warm the cache the request path answers from`() {
    val fetcher = mock<CimdMetadataFetcher> { on { fetchAndValidate(any(), any()) } doReturn resolved(URL) }
    val cache = newCache(fetcher)

    cache.fetchOnGrantLane(URL)

    cache.cachedResolution(URL).assert.isNull()
    cache.get(URL).assert.isNotNull()
    verify(fetcher, times(2)).fetchAndValidate(eq(URL), any())
  }

  @Test
  fun `the check reads the publisher on every round, never its own last answer`() {
    val fetcher = mock<CimdMetadataFetcher> { on { fetchAndValidate(any(), any()) } doReturn resolved(URL) }
    val cache = newCache(fetcher)

    repeat(3) { cache.fetchOnGrantLane(URL) }

    verify(fetcher, times(3)).fetchAndValidate(eq(URL), any())
  }

  @Test
  fun `a capacity refusal answers unavailable rather than something the caller could act on`() {
    val fetcher =
      mock<CimdMetadataFetcher> {
        on { fetchAndValidate(any(), any()) } doAnswer { throw CimdNoCapacityException(URL) }
      }
    val cache = newCache(fetcher)

    cache.fetchOnGrantLane(URL).assert.isEqualTo(CimdResolution.Unavailable)
    metrics.oauth2CimdCapacityRefusalsCounter
      .count()
      .assert
      .isEqualTo(1.0)
  }

  @Test
  fun `a capacity refusal is not remembered, so the next caller still finds out`() {
    val attempts = AtomicInteger()
    val fetcher =
      mock<CimdMetadataFetcher> {
        on { fetchAndValidate(any(), any()) } doAnswer {
          if (attempts.incrementAndGet() == 1) throw CimdNoCapacityException(URL)
          CimdResolution.Withdrawn
        }
      }
    val cache = newCache(fetcher)

    cache.get(URL)
    cache.cachedResolution(URL).assert.isNull()
    cache.get(URL)
    cache.cachedResolution(URL).assert.isEqualTo(CimdResolution.Withdrawn)
    attempts.get().assert.isEqualTo(2)
  }

  @Test
  fun `a lookup for an existing grant gets through a budget an unauthenticated caller has filled`() {
    val release = CountDownLatch(1)
    val started = CountDownLatch(1)
    val fetcher =
      mock<CimdMetadataFetcher> {
        on { fetchAndValidate(any(), any()) } doAnswer {
          if (it.getArgument<CimdFetchLane>(1) == CimdFetchLane.GRANT_CHECK) return@doAnswer CimdResolution.Withdrawn
          started.countDown()
          release.await()
          resolved(URL)
        }
      }
    val cache = newCache(fetcher, budget = CimdFetchBudget(maxConcurrent = 1, maxConcurrentPerOrigin = 1))
    val pool = Executors.newSingleThreadExecutor()
    pool.submit { cache.get("https://other.example/client") }
    started.await(10, TimeUnit.SECONDS).assert.isTrue()

    cache.fetchOnGrantLane(URL).assert.isEqualTo(CimdResolution.Withdrawn)

    release.countDown()
    pool.shutdown()
    pool.awaitTermination(10, TimeUnit.SECONDS)
  }

  @Test
  fun `a withdrawal outlives the short-lived unavailable answers a saturating caller can produce`() {
    val clock = AtomicLong(0)
    val fetcher = mock<CimdMetadataFetcher> { on { fetchAndValidate(any(), any()) } doReturn CimdResolution.Withdrawn }
    val cache = newCache(fetcher, ticker = clock::get)

    cache.get(URL)
    // Far past the 5s an "unavailable" answer lives for.
    clock.set(seconds(120))
    cache.cachedResolution(URL).assert.isEqualTo(CimdResolution.Withdrawn)

    verify(fetcher, times(1)).fetchAndValidate(any(), any())
  }

  @Test
  fun `a withdrawal the check found is not visible to the request path's cache`() {
    val fetcher = mock<CimdMetadataFetcher> { on { fetchAndValidate(any(), any()) } doReturn CimdResolution.Withdrawn }
    val cache = newCache(fetcher)

    cache.fetchOnGrantLane(URL)

    cache.cachedResolution(URL).assert.isNull()
  }

  private fun newCache(
    fetcher: CimdMetadataFetcher,
    budget: CimdFetchBudget = CimdFetchBudget(),
    approximateMaxEntries: Long = CimdClientCache.MAX_ENTRIES,
    positiveTtlSeconds: Long = CimdClientCache.POSITIVE_TTL_SECONDS,
    unavailableTtlSeconds: Long = CimdClientCache.UNAVAILABLE_TTL_SECONDS,
    ticker: Ticker = Ticker.systemTicker(),
  ) = CimdClientCache(
    fetcher,
    metrics,
    budget,
    approximateMaxEntries = approximateMaxEntries,
    positiveTtlSeconds = positiveTtlSeconds,
    unavailableTtlSeconds = unavailableTtlSeconds,
    ticker = ticker,
  )

  private fun seconds(value: Long): Long = value * 1_000_000_000L

  private fun resolved(url: String) =
    CimdResolution.Resolved(
      client(url),
    )

  private fun client(url: String) =
    CimdClient(
      OAuth2Client(clientId = url, name = url, redirectUris = listOf("$url/cb"), verified = false, metadataHash = "h"),
      clientOrigin = url,
    )

  companion object {
    private const val URL = "https://app.example.com/.well-known/oauth-client"
  }
}
