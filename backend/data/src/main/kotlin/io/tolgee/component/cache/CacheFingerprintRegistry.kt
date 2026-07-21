package io.tolgee.component.cache

import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.stereotype.Component
import org.springframework.util.ClassUtils
import java.lang.reflect.Method
import kotlin.reflect.KType
import kotlin.reflect.jvm.kotlinFunction

/**
 * Maps each cache name to a structural fingerprint of the value it stores, so a cache can be
 * namespaced by shape. When a cached type's shape changes between versions, the fingerprint changes,
 * the physical cache name changes, and stale entries are simply never read again (they expire by TTL)
 * instead of being silently mis-deserialized.
 *
 * The value type of each cache comes from one of two sources, both resolved once at first access:
 * `@Cacheable`/`@CachePut` methods (discovered by return type) and [DirectAccessCacheTypeProvider]s
 * (declared types for caches used without an annotation). Because every cache name resolves through
 * the single [physicalName], plain `cacheManager.getCache(name)` reaches the right physical cache
 * everywhere — no per-call-site type wiring.
 */
@Component
class CacheFingerprintRegistry(
  private val applicationContext: ApplicationContext,
  private val fingerprint: CacheValueFingerprint,
  private val directAccessCacheTypeProviders: List<DirectAccessCacheTypeProvider>,
) {
  private val logger = LoggerFactory.getLogger(CacheFingerprintRegistry::class.java)

  private val fingerprintByCacheName: Map<String, String> by lazy { buildFingerprints() }

  fun physicalName(cacheName: String): String {
    if (cacheName.contains(SEPARATOR)) return cacheName
    val fp = fingerprintByCacheName[cacheName] ?: return cacheName
    return "$cacheName$SEPARATOR$fp"
  }

  private fun buildFingerprints(): Map<String, String> {
    val fingerprints = HashMap<String, String>()
    directAccessCacheTypeProviders.forEach { provider ->
      provider.getDirectAccessCacheTypes().forEach { (cacheName, valueType) ->
        fingerprints[cacheName] = fingerprint.compute(valueType)
      }
    }
    scanAnnotatedCaches().forEach { (cacheName, fp) ->
      fingerprints[cacheName]?.let {
        logger.warn(
          "Cache '{}' is both annotation-discovered and directly declared; using the annotation type",
          cacheName,
        )
      }
      fingerprints[cacheName] = fp
    }
    return fingerprints
  }

  private fun scanAnnotatedCaches(): Map<String, String> {
    val returnTypesByCache = HashMap<String, MutableSet<KType>>()
    applicationContext.beanDefinitionNames.forEach { beanName ->
      val beanType = runCatching { applicationContext.getType(beanName) }.getOrNull() ?: return@forEach
      val methods = runCatching { ClassUtils.getUserClass(beanType).methods }.getOrNull() ?: return@forEach
      methods.forEach { method ->
        val cacheNames = cacheNamesOf(method)
        if (cacheNames.isEmpty()) return@forEach
        // Resolving the Kotlin return type loads its class graph; a bean whose signature references
        // an optional dependency absent at runtime must not abort the whole scan.
        val returnType =
          runCatching { method.kotlinFunction?.returnType }.getOrElse {
            logger.warn(
              "Could not resolve return type of cached method {}; its cache stays un-fingerprinted",
              method,
              it,
            )
            null
          } ?: return@forEach
        cacheNames.forEach { cacheName ->
          returnTypesByCache.getOrPut(cacheName) { HashSet() }.add(returnType)
        }
      }
    }
    return returnTypesByCache.mapValues { (_, types) -> fingerprint.compute(types) }
  }

  private fun cacheNamesOf(method: Method): Set<String> {
    val names = LinkedHashSet<String>()
    AnnotatedElementUtils
      .findMergedAnnotation(method, Cacheable::class.java)
      ?.let { names.addAll(it.cacheNames.toList()) }
    AnnotatedElementUtils
      .findMergedAnnotation(method, CachePut::class.java)
      ?.let { names.addAll(it.cacheNames.toList()) }
    return names
  }

  companion object {
    const val SEPARATOR = "--"
  }
}
