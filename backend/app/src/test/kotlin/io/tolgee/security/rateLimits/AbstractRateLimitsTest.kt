package io.tolgee.security.rateLimits

import io.tolgee.component.CurrentDateProvider
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
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*
import kotlin.system.measureTimeMillis

abstract class AbstractRateLimitsTest : AuthorizedControllerTest() {

  @AfterEach
  fun teardown() {
    super.clearCaches()
  }

  @MockBean
  @Autowired
  lateinit var rateLimitParamsProxy: RateLimitParamsProxy

  @SpyBean
  @Autowired
  lateinit var currentDateProvider: CurrentDateProvider

  private val oneHour = 60 * 60 * 1000
  private val threads = 10
  private val repeat = 10
  private val bucketSize = threads * repeat

  /**
   * Utility method helping to test the rate limits
   */
  protected fun testRateLimit(
    keyPrefix: String,
    expectedStatus: Int = 200,
    performAction: () -> ResultActions
  ) {
    val startDate = initMocks(bucketSize, keyPrefix)
    emptyBucketByRunningManyConcurrentRequests(performAction, expectedStatus)
    checkThrowOutOfCredits(performAction, keyPrefix)
    checkItRefillsBucket(startDate, performAction, expectedStatus)
    performAction().andExpect { status().`is`(expectedStatus) }
  }

  /**
   * Utility method helping to test the rate limits
   */
  protected fun testRateLimitDisabled(
    keyPrefix: String,
    expectedStatus: Int = 200,
    performAction: () -> ResultActions
  ) {
    initMocks(bucketSize, keyPrefix)
    emptyBucketByRunningManyConcurrentRequests(performAction, expectedStatus)
  }

  private fun checkItRefillsBucket(
    startDate: Date,
    performAction: () -> ResultActions,
    expectedStatus: Int
  ) {
    whenever(currentDateProvider.date).thenReturn(Date(startDate.time + oneHour + 1))
    performAction().andExpect { status().`is`(expectedStatus) }
  }

  private fun checkThrowOutOfCredits(
    performAction: () -> ResultActions,
    keyPrefix: String
  ) {
    performAction().andIsBadRequest.andAssertThatJson {
      node("params[0]") {
        node("bucketSize").isEqualTo(bucketSize)
        node("timeToRefill").isEqualTo(oneHour)
        node("keyPrefix").isEqualTo(keyPrefix)
      }
    }
  }

  private fun emptyBucketByRunningManyConcurrentRequests(
    performAction: () -> ResultActions,
    expectedStatus: Int
  ) {
    measureTimeMillis {
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
  }

  private fun initMocks(bucketSize: Int, keyPrefix: String): Date {
    val startDate = Date()
    whenever(currentDateProvider.date).thenReturn(startDate)
    // all other limits have to pass, so we have to set larger values
    whenever(rateLimitParamsProxy.getBucketSize(any(), any())).thenReturn(bucketSize + 20000)
    whenever(rateLimitParamsProxy.getTimeToRefill(any(), any())).thenReturn(oneHour + 20000)

    // mock values for provided key prefix
    whenever(rateLimitParamsProxy.getBucketSize(eq(keyPrefix), any())).thenReturn(bucketSize)
    whenever(rateLimitParamsProxy.getTimeToRefill(eq(keyPrefix), any())).thenReturn(oneHour)
    return startDate
  }
}
