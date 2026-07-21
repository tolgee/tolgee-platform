package io.tolgee.component.cache

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.SmartInitializingSingleton
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
 * Resolves each cache name to a structural fingerprint of its stored value type — from
 * `@Cacheable`/`@CachePut` return types and [DirectAccessCacheTypeProvider] declarations — exposed
 * through [physicalName]. A name with no known type is returned unchanged (un-fingerprinted).
 */
@Component
class CacheFingerprintRegistry(
  private val applicationContext: ApplicationContext,
  private val fingerprint: CacheValueFingerprint,
  private val directAccessCacheTypeProviders: List<DirectAccessCacheTypeProvider>,
) : SmartInitializingSingleton {
  private val logger = LoggerFactory.getLogger(CacheFingerprintRegistry::class.java)

  // Reflecting over cached methods needs every singleton to exist, so the build cannot run before
  // afterSingletonsInstantiated — building earlier would resolve against a half-initialized context.
  private var fingerprintByCacheName: Map<String, String>? = null

  override fun afterSingletonsInstantiated() {
    val built = buildFingerprints()
    fingerprintByCacheName = built
    logger.info("Fingerprinted caches: {}", built.keys.sorted())
  }

  fun physicalName(cacheName: String): String {
    if (cacheName.contains(SEPARATOR)) return cacheName
    val fingerprints =
      fingerprintByCacheName
        ?: throw IllegalStateException("Cache '$cacheName' accessed before the fingerprint registry was built")
    val fp = fingerprints[cacheName] ?: return cacheName
    return "$cacheName$SEPARATOR$fp"
  }

  private fun buildFingerprints(): Map<String, String> {
    val fingerprints = HashMap<String, String>()
    directAccessCacheTypeProviders.forEach { provider ->
      provider.getDirectAccessCacheTypes().forEach { (cacheName, valueType) ->
        computeFingerprint(cacheName) { fingerprint.compute(valueType) }?.let { fingerprints[cacheName] = it }
      }
    }
    scanAnnotatedCaches().forEach { (cacheName, fp) ->
      if (fingerprints.containsKey(cacheName)) {
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
      // getMethods() is public-only; @Cacheable is often on a protected self-invocation method, so
      // walk declared methods up the hierarchy too.
      val methods = runCatching { cacheableMethods(ClassUtils.getUserClass(beanType)) }.getOrNull() ?: return@forEach
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
    return returnTypesByCache
      .mapNotNull { (cacheName, types) ->
        computeFingerprint(cacheName) { fingerprint.compute(types) }?.let { cacheName to it }
      }.toMap()
  }

  private fun computeFingerprint(
    cacheName: String,
    compute: () -> String,
  ): String? =
    runCatching(compute).getOrElse {
      logger.warn("Could not fingerprint cache '{}'; it stays un-fingerprinted", cacheName, it)
      null
    }

  private fun cacheableMethods(userClass: Class<*>): List<Method> {
    val methods = LinkedHashSet<Method>()
    var current: Class<*>? = userClass
    while (current != null && current != Any::class.java) {
      methods.addAll(current.declaredMethods)
      current = current.superclass
    }
    methods.addAll(userClass.methods)
    return methods.toList()
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
