package io.tolgee.security.oauth2.cimd

import io.tolgee.security.oauth2.OAuth2Client
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class CimdClientCacheTest {
  @Test
  fun `a resolved client is fetched once and then served from the cache`() {
    val fetcher = mock<CimdMetadataFetcher> { on { fetchAndValidate(any()) } doReturn client(URL) }
    val cache = CimdClientCache(fetcher)

    repeat(5) { cache.get(URL).assert.isNotNull() }

    verify(fetcher, times(1)).fetchAndValidate(URL)
  }

  @Test
  fun `N concurrent cold requests for one client_id trigger exactly one fetch`() {
    val calls = AtomicInteger(0)
    val release = CountDownLatch(1)
    val fetcher =
      mock<CimdMetadataFetcher> {
        on { fetchAndValidate(any()) } doAnswer {
          calls.incrementAndGet()
          release.await()
          client(URL)
        }
      }
    val cache = CimdClientCache(fetcher)

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
  fun `the cache is bounded at its cap under attacker-chosen key churn`() {
    val fetcher =
      mock<CimdMetadataFetcher> {
        on { fetchAndValidate(any()) } doAnswer { client(it.getArgument(0)) }
      }
    val cache = CimdClientCache(fetcher, maxSize = 10)

    repeat(1000) { cache.get("https://churn-$it.example/client") }

    cache.estimatedSize().assert.isLessThanOrEqualTo(10)
  }

  @Test
  fun `an unresolved client_id is cached as a negative rather than refetched every time`() {
    val fetcher = mock<CimdMetadataFetcher> { on { fetchAndValidate(any()) } doReturn null }
    val cache = CimdClientCache(fetcher)

    repeat(5) { cache.get(URL).assert.isNull() }

    verify(fetcher, times(1)).fetchAndValidate(URL)
  }

  private fun client(url: String) =
    CimdClient(
      OAuth2Client(clientId = url, name = url, redirectUris = listOf("$url/cb"), verified = false, metadataHash = "h"),
      logoUri = null,
    )

  companion object {
    private const val URL = "https://app.example.com/.well-known/oauth-client"
  }
}
