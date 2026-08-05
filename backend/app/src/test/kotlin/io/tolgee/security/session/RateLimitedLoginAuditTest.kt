package io.tolgee.security.session

import io.tolgee.constants.Caches
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.repository.AuthAuditEventRepository
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import java.util.Date

/**
 * Rate limiting only engages when the cache is on, which the default test context does not do.
 */
@AutoConfigureMockMvc
@ContextRecreatingTest
@SpringBootTest(properties = ["tolgee.cache.enabled=true"])
class RateLimitedLoginAuditTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var authAuditEventRepository: AuthAuditEventRepository

  lateinit var testData: BaseTestData

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    testDataService.saveTestData(testData.root)
    cacheManager.getCache(Caches.RATE_LIMITS)?.clear()
    // frozen clock, so the one second bucket cannot refill mid burst
    setForcedDate(Date())
  }

  @AfterEach
  fun cleanup() {
    cacheManager.getCache(Caches.RATE_LIMITS)?.clear()
    clearForcedDate()
  }

  @Test
  fun `rate limited attempts are not recorded`() {
    var reachedHandler = 0
    var rateLimited = false

    repeat(MAX_LOGIN_ATTEMPTS) {
      val status = attemptLogin().andReturn().response.status
      if (status == 429) {
        rateLimited = true
      }
      if (status == 401) {
        reachedHandler++
      }
    }

    rateLimited.assert.isTrue()
    // only the attempts that got as far as the handler leave a trace
    failureEvents().assert.hasSize(reachedHandler)
  }

  private fun failureEvents() = authAuditEventRepository.findAll().filter { it.type.name.startsWith("LOGIN_FAILED") }

  private fun attemptLogin() =
    perform(
      MockMvcRequestBuilders
        .post("/api/public/generatetoken")
        .contentType(MediaType.APPLICATION_JSON)
        .content(
          mapper.writeValueAsString(
            mapOf("username" to testData.user.username, "password" to "wrong"),
          ),
        ),
    )

  companion object {
    const val MAX_LOGIN_ATTEMPTS = 10
  }
}
