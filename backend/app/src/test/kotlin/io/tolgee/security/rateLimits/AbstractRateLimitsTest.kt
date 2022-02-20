package io.tolgee.security.rateLimits

import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.node
import io.tolgee.testing.AuthorizedControllerTest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cache.CacheManager
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.system.measureTimeMillis

abstract class AbstractRateLimitsTest : AuthorizedControllerTest() {

  @Autowired
  lateinit var cacheManager: CacheManager

  @AfterEach
  fun clearCaches() {
    cacheManager.cacheNames.stream().forEach { cacheName: String ->
      @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
      cacheManager.getCache(cacheName).clear()
    }
  }

  @MockBean
  @Autowired
  lateinit var rateLimitParamsProxy: RateLimitParamsProxy

  /**
   * Utility method helping to test the rate limits
   */
  protected fun testEndpoint(
    threads: Int,
    repeat: Int,
    keyPrefix: String,
    timeToRefill: Int,
    expectedStatus: Int = 200,
    performAction: () -> ResultActions
  ) {
    val bucketSize = threads * repeat + 1
    // all other limits have to pass, so we have to set larger values
    whenever(rateLimitParamsProxy.getBucketSize(any(), any())).thenReturn(bucketSize + 20000)
    whenever(rateLimitParamsProxy.getTimeToRefill(any(), any())).thenReturn(timeToRefill + 20000)

    // mock values for provided key prefix
    whenever(rateLimitParamsProxy.getBucketSize(eq(keyPrefix), any())).thenReturn(bucketSize)
    whenever(rateLimitParamsProxy.getTimeToRefill(eq(keyPrefix), any())).thenReturn(timeToRefill)

    // init the bucket before time measurement, so we can subtract it afterwards
    performAction().andExpect { status().`is`(expectedStatus) }
    val executionTime = measureTimeMillis {
      runBlocking {
        repeat(threads) {
          launch {
            repeat(repeat) {
              performAction().andExpect { status().`is`(expectedStatus) }
            }
          }
        }
      }
    }
    performAction().andIsBadRequest.andAssertThatJson {
      node("params[0]") {
        node("bucketSize").isEqualTo(bucketSize)
        node("timeToRefill").isEqualTo(1000)
        node("keyPrefix").isEqualTo(keyPrefix)
      }
    }
    Thread.sleep(1000 - executionTime)
    performAction().andExpect { status().`is`(expectedStatus) }
  }
}
