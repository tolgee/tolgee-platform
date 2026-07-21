package io.tolgee.cache

import io.tolgee.component.cache.CacheFingerprintRegistry
import io.tolgee.constants.Caches
import io.tolgee.fixtures.RedisRunner
import io.tolgee.model.UserAccount
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.redisson.api.RedissonClient
import org.redisson.client.codec.ByteArrayCodec
import org.redisson.codec.CompositeCodec
import org.redisson.spring.cache.RedissonSpringCacheManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.cache.use-redis=true",
    "tolgee.cache.enabled=true",
    "tolgee.internal.fake-mt-providers=false",
    "tolgee.machine-translation.free-credits-amount=10000000",
  ],
)
@ContextConfiguration(initializers = [CacheWithRedisTest.Companion.Initializer::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CacheWithRedisTest : AbstractCacheTest() {
  companion object {
    val redisRunner = RedisRunner()

    @AfterAll
    @JvmStatic
    fun cleanup() {
      redisRunner.stop()
    }

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
      override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        redisRunner.run()
        TestPropertyValues
          .of("spring.data.redis.port=${RedisRunner.port}")
          .applyTo(configurableApplicationContext)
      }
    }
  }

  @Autowired
  lateinit var redissonClient: RedissonClient

  @Autowired
  lateinit var cacheFingerprintRegistry: CacheFingerprintRegistry

  @Test
  fun `it has proper cache manager`() {
    assertThat(unwrappedCacheManager).isInstanceOf(RedissonSpringCacheManager::class.java)
  }

  private data class OldShape(
    val a: Int,
  )

  private data class NewShape(
    val a: Int,
    val b: Long,
  )

  @Test
  fun `a shape change routes to a fresh physical cache without cross-version reads`() {
    val cacheName = "fingerprintIsolationTestCache"
    val oldPhysical = cacheFingerprintRegistry.physicalName(cacheName, OldShape::class)
    val newPhysical = cacheFingerprintRegistry.physicalName(cacheName, NewShape::class)
    oldPhysical.assert.isNotEqualTo(newPhysical)

    cacheManager.getCache(oldPhysical)!!.put("k", "old-value")

    // The new app version computes a different fingerprint -> different physical cache -> miss.
    cacheManager
      .getCache(newPhysical)!!
      .get("k", String::class.java)
      .assert
      .isNull()
    // The old entry stays addressable under the old fingerprint, so rolling deploys coexist.
    cacheManager
      .getCache(oldPhysical)!!
      .get("k", String::class.java)
      .assert
      .isEqualTo("old-value")
  }

  @Test
  fun `heals a cache entry that can no longer be deserialized after a shape change`() {
    val userId = 4242L
    val user =
      UserAccount().apply {
        name = "Schema Change User"
        id = userId
      }
    whenever(userAccountRepository.findActive(userId)).then { user }

    cacheManager.getCache(Caches.USER_ACCOUNTS)!!.clear()

    userAccountService.findDto(userId).assert.isNotNull
    verify(userAccountRepository, times(1)).findActive(userId)

    corruptStoredValue(Caches.USER_ACCOUNTS, userId)

    userAccountService.findDto(userId).assert.isNotNull
    verify(userAccountRepository, times(2)).findActive(userId)

    userAccountService.findDto(userId).assert.isNotNull
    verify(userAccountRepository, times(2)).findActive(userId)
  }

  /**
   * Truncates the raw bytes stored under a cache key so the codec can no longer deserialize them,
   * reproducing the KryoBufferUnderflowException that a cached object's shape change between
   * versions produces. The composite codec keeps the map-key encoding identical to the cache's,
   * so the write lands on the exact same Redis hash field.
   */
  private fun corruptStoredValue(
    cacheName: String,
    key: Any,
  ) {
    val rawMap =
      redissonClient.getMap<Any, ByteArray>(
        cacheFingerprintRegistry.physicalName(cacheName),
        CompositeCodec(redissonClient.config.codec, ByteArrayCodec.INSTANCE),
      )
    val stored = rawMap[key]
    stored.assert.isNotNull
    rawMap[key] = stored!!.copyOf(stored.size / 2)
  }
}
