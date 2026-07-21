package io.tolgee.component.cache

import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.stereotype.Component
import org.springframework.util.ClassUtils
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.kotlinFunction

/**
 * Maps each cache name to a structural fingerprint of the value it stores, so a cache can be
 * namespaced by shape. When a cached type's shape changes between versions, the fingerprint changes,
 * the physical cache name changes, and stale entries are simply never read again (they expire by TTL)
 * instead of being silently mis-deserialized.
 *
 * Fingerprints for `@Cacheable`/`@CachePut` caches are discovered automatically from the annotated
 * methods' return types. Caches accessed directly (no annotation) resolve their physical name through
 * [physicalName] with the value type supplied at the call site.
 *
 * The scan is lazy: it runs on first cache access, once the application context is fully initialized.
 */
@Component
class CacheFingerprintRegistry(
  private val applicationContext: ApplicationContext,
  private val fingerprint: CacheValueFingerprint,
) {
  private val fingerprintByCacheName: Map<String, String> by lazy { scanAnnotatedCaches() }

  // Direct-access overloads run on per-request hot paths; the fingerprint of a type never changes
  // within a running JVM, so memoize it rather than repeating the reflective walk on every call.
  private val fingerprintByType = ConcurrentHashMap<KType, String>()
  private val fingerprintByClass = ConcurrentHashMap<KClass<*>, String>()

  fun physicalName(cacheName: String): String {
    if (cacheName.contains(SEPARATOR)) return cacheName
    val fp = fingerprintByCacheName[cacheName] ?: return cacheName
    return "$cacheName$SEPARATOR$fp"
  }

  fun physicalName(
    cacheName: String,
    valueType: KType,
  ): String = "$cacheName$SEPARATOR${fingerprintByType.getOrPut(valueType) { fingerprint.compute(valueType) }}"

  fun physicalName(
    cacheName: String,
    valueType: KClass<*>,
  ): String = "$cacheName$SEPARATOR${fingerprintByClass.getOrPut(valueType) { fingerprint.compute(valueType) }}"

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
        val returnType = runCatching { method.kotlinFunction?.returnType }.getOrNull() ?: return@forEach
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
      .findMergedAnnotation(
        method,
        Cacheable::class.java,
      )?.let { names.addAll(it.cacheNames.toList()) }
    AnnotatedElementUtils
      .findMergedAnnotation(
        method,
        CachePut::class.java,
      )?.let { names.addAll(it.cacheNames.toList()) }
    return names
  }

  companion object {
    const val SEPARATOR = "--"
  }
}
