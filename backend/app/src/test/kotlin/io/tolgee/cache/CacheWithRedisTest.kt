package io.tolgee.cache

import io.netty.buffer.Unpooled
import io.tolgee.component.EnumNameKryo5Codec
import io.tolgee.component.ProjectTranslationLastModifiedManager
import io.tolgee.component.ResilientCacheAccessor
import io.tolgee.component.cache.CacheFingerprintRegistry
import io.tolgee.component.cache.CacheValueFingerprint
import io.tolgee.component.machineTranslation.TranslateResult
import io.tolgee.constants.Caches
import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.cacheable.PermissionDto
import io.tolgee.fixtures.RedisRunner
import io.tolgee.model.Permission
import io.tolgee.model.UserAccount
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.redisson.api.RedissonClient
import org.redisson.client.codec.ByteArrayCodec
import org.redisson.client.handler.State
import org.redisson.codec.CompositeCodec
import org.redisson.codec.Kryo5Codec
import org.redisson.spring.cache.CacheConfig
import org.redisson.spring.cache.RedissonSpringCacheManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.cache.Cache
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
    private const val KEY = "permission-key"

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

  @Autowired
  lateinit var cacheValueFingerprint: CacheValueFingerprint

  @Autowired
  lateinit var resilientCacheAccessor: ResilientCacheAccessor

  @Autowired
  lateinit var projectTranslationLastModifiedManager: ProjectTranslationLastModifiedManager

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
    val oldPhysical = cacheName + CacheFingerprintRegistry.SEPARATOR + cacheValueFingerprint.compute(OldShape::class)
    val newPhysical = cacheName + CacheFingerprintRegistry.SEPARATOR + cacheValueFingerprint.compute(NewShape::class)
    oldPhysical.assert.isNotEqualTo(newPhysical)

    cacheManager.getCache(oldPhysical)!!.put("k", "old-value")

    cacheManager
      .getCache(newPhysical)!!
      .get("k", String::class.java)
      .assert
      .isNull()
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

  @Test
  fun `heals a corrupt project-translations-modified entry read directly`() {
    val projectId = 9182L
    val first = projectTranslationLastModifiedManager.getLastModifiedInfo(projectId)

    corruptStoredValue(Caches.PROJECT_TRANSLATIONS_MODIFIED, projectId)

    val healed = projectTranslationLastModifiedManager.getLastModifiedInfo(projectId)
    healed.assert.isNotEqualTo(first)
  }

  @Test
  fun `writes enum names into redis`() {
    permissionsCache.put(KEY, permissionDtoFixture)

    val stored = rawPermissionsCache.readAllValues().map { it.stripKryoHighBits() }
    assertThat(stored).hasSize(1)
    assertThat(stored.single()).contains("ADMIN")
    assertThat(permissionsCache.get(KEY)!!.get()).isEqualTo(permissionDtoFixture)
  }

  @Test
  fun `evicts and misses when ResilientCacheAccessor reads a previous-deploy entry`() {
    previousDeployPermissionsCache.put(KEY, permissionDtoFixture)
    assertThat(rawPermissionsCache.readAllValues()).hasSize(1)

    val value = resilientCacheAccessor.get(permissionsCache, KEY, PermissionDto::class.java)

    assertThat(value).isNull()
    assertThat(rawPermissionsCache.readAllValues()).isEmpty()
  }

  @Test
  fun `falls back to the repository when the Cacheable path reads a previous-deploy entry`() {
    whenever(permissionRepository.findOneByProjectIdAndUserIdAndOrganizationId(3, 2)).then { Permission(id = 1) }
    previousDeployPermissionsCache.put(arrayListOf(2L, 3L, null), permissionDtoFixture)
    assertThat(rawPermissionsCache.readAllValues()).hasSize(1)

    val found = permissionService.find(projectId = 3, userId = 2)

    assertThat(found).isNotNull
    verify(permissionRepository, times(1)).findOneByProjectIdAndUserIdAndOrganizationId(3, 2)
    assertThat(rawPermissionsCache.readAllValues())
      .describedAs("stale entry evicted and recomputed under the same key")
      .hasSize(1)
  }

  @Test
  fun `falls back to the provider when the machine translation cache holds a previous-deploy entry`() {
    doAnswer { googleResponse }.whenever(awsTranslationProvider).translate(any())
    mtServiceManager.translate(paramsEnAws)
    verify(awsTranslationProvider, times(1)).translate(any())

    previousDeployCache(Caches.MACHINE_TRANSLATIONS).put(
      machineTranslationCacheKey(),
      TranslateResult(translatedText = "Hello", usedService = MtServiceType.AWS),
    )

    mtServiceManager.translate(paramsEnAws)

    verify(awsTranslationProvider, times(2)).translate(any())
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
        CompositeCodec(EnumNameKryo5Codec(), ByteArrayCodec.INSTANCE),
      )
    val stored = rawMap[key]
    stored.assert.isNotNull
    rawMap[key] = stored!!.copyOf(stored.size / 2)
  }

  private fun machineTranslationCacheKey(): String {
    val rawKey = rawCache(Caches.MACHINE_TRANSLATIONS).readAllKeySet().single()
    return EnumNameKryo5Codec().valueDecoder.decode(Unpooled.wrappedBuffer(rawKey), State()) as String
  }

  private val permissionsCache get() = cacheManager.getCache(Caches.PERMISSIONS)!!

  private val rawPermissionsCache get() = rawCache(Caches.PERMISSIONS)

  private fun rawCache(name: String) =
    redissonClient.getMapCache<ByteArray, ByteArray>(
      cacheFingerprintRegistry.physicalName(name),
      ByteArrayCodec.INSTANCE,
    )

  private val previousDeployPermissionsCache get() = previousDeployCache(Caches.PERMISSIONS)

  private fun previousDeployCache(name: String): Cache {
    val physicalName = cacheFingerprintRegistry.physicalName(name)
    return RedissonSpringCacheManager(
      redissonClient,
      mapOf(physicalName to CacheConfig(tolgeeProperties.cache.defaultTtl, tolgeeProperties.cache.defaultTtl)),
      Kryo5Codec(),
    ).getCache(physicalName)!!
  }
}
