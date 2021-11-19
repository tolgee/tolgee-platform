package io.tolgee.security.rateLimits

import io.tolgee.controllers.AbstractControllerTest
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.testng.annotations.BeforeMethod
import kotlin.system.measureTimeMillis

abstract class AbstractRateLimitsTest : AbstractControllerTest() {
  @Autowired
  @MockBean
  lateinit var rateLimitsManager: RateLimitsManager

  @BeforeMethod
  fun setup() {
    whenever(rateLimitsManager.rateLimits).thenReturn(
      listOf(
        RateLimit(
          urlMatcher = Regex("/.*"),
          keyPrefix = "ip",
          keyProvider = { it.remoteAddr },
          bucketSizeProvider = { 17 },
          timeToRefillInMs = 1000,
        )
      )
    )
  }

  fun `it doesn't allow more then set in configuration`() {
    // init the bucket before time measurement, so we can subtract it afterwards
    performGet("/api/public/configuration").andIsOk
    val time = measureTimeMillis {
      runBlocking {
        repeat(4) {
          launch {
            repeat(4) {
              performGet("/api/public/configuration").andIsOk
            }
          }
        }
      }
    }
    performGet("/api/public/configuration").andIsBadRequest.andPrettyPrint
    Thread.sleep(1000 - time)
    performGet("/api/public/configuration").andIsOk
  }
}
