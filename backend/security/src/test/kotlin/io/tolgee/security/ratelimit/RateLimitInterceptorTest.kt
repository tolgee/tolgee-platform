package io.tolgee.security.ratelimit

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.LockingProvider
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsRateLimited
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Date
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class RateLimitInterceptorTest {
  private val rateLimitProperties = Mockito.spy(RateLimitProperties::class.java)

  private val currentDateProvider = Mockito.mock(CurrentDateProvider::class.java)

  private val rateLimitService = Mockito.spy(
    RateLimitService(
      ConcurrentMapCacheManager(),
      TestLockingProvider(),
      currentDateProvider,
      rateLimitProperties,
    )
  )

  private val rateLimitInterceptor = RateLimitInterceptor(rateLimitService)

  private val mockMvc = MockMvcBuilders.standaloneSetup(TestController::class.java)
    .addInterceptors(rateLimitInterceptor)
    .build()

  @BeforeEach
  fun setupMocks() {
    Mockito.`when`(currentDateProvider.date).thenReturn(Date())
  }

  @AfterEach
  fun resetMocks() {
    Mockito.reset(rateLimitProperties, currentDateProvider)
  }

  @Test
  fun `it does not rate limit when there are no annotations`() {
    mockMvc.perform(get("/")).andIsOk
    mockMvc.perform(get("/")).andIsOk
    mockMvc.perform(get("/")).andIsOk

    Mockito.verify(rateLimitService, Mockito.never()).consumeBucket(any())
  }

  @Test
  fun `it rate limits requests according to the specified policy`() {
    mockMvc.perform(get("/rate-limited")).andIsOk
    mockMvc.perform(get("/rate-limited")).andIsOk
    mockMvc.perform(get("/rate-limited")).andIsRateLimited

    Mockito.verify(rateLimitService, Mockito.times(3)).consumeBucket(any())
  }

  @Test
  fun `it uses different buckets for different paths`() {
    mockMvc.perform(get("/rate-limited")).andIsOk
    mockMvc.perform(get("/rate-limited")).andIsOk
    mockMvc.perform(get("/rate-limited")).andIsRateLimited

    mockMvc.perform(get("/rate-limited-2")).andIsOk
    mockMvc.perform(get("/rate-limited-2")).andIsOk
    mockMvc.perform(get("/rate-limited-2")).andIsRateLimited
  }

  @Test
  fun `it uses the same buckets for paths with a shared bucket`() {
    mockMvc.perform(get("/rate-limited-shared")).andIsOk
    mockMvc.perform(get("/rate-limited-shared")).andIsOk
    mockMvc.perform(get("/rate-limited-shared")).andIsRateLimited
    mockMvc.perform(get("/rate-limited-shared-2")).andIsRateLimited

    val now = currentDateProvider.date.time
    Mockito.`when`(currentDateProvider.date).thenReturn(Date(now + 5000))

    mockMvc.perform(get("/rate-limited-shared")).andIsOk
    mockMvc.perform(get("/rate-limited-shared-2")).andIsOk
    mockMvc.perform(get("/rate-limited-shared")).andIsRateLimited
    mockMvc.perform(get("/rate-limited-shared-2")).andIsRateLimited
  }

  @Test
  fun `it does not rate limit when limits are disabled`() {
    Mockito.`when`(rateLimitProperties.endpointLimits).thenReturn(false)
    mockMvc.perform(get("/rate-limited")).andIsOk
    mockMvc.perform(get("/rate-limited")).andIsOk
    mockMvc.perform(get("/rate-limited")).andIsOk
    mockMvc.perform(get("/rate-limited")).andIsOk

    Mockito.verify(rateLimitService, Mockito.never()).consumeBucket(any())
  }

  class TestLockingProvider : LockingProvider {
    private val lock = ReentrantLock()
    override fun getLock(name: String): Lock = lock
  }

  @RestController
  class TestController {
    @GetMapping("/")
    fun noRateLimit(): String {
      return "henlo!"
    }

    @GetMapping("/rate-limited")
    @RateLimited(2)
    fun rateLimited(): String {
      return "henlo!"
    }

    @GetMapping("/rate-limited-2")
    @RateLimited(2)
    fun rateLimited2(): String {
      return "henlo!"
    }

    @GetMapping("/rate-limited-shared")
    @RateLimited(2, bucketName = "shared")
    fun rateLimitedShared1(): String {
      return "henlo!"
    }

    @GetMapping("/rate-limited-shared-2")
    @RateLimited(2, bucketName = "shared")
    fun rateLimitedShared2(): String {
      return "henlo!"
    }
  }
}
