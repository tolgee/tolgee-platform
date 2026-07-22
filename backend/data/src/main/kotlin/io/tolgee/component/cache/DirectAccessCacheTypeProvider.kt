package io.tolgee.component.cache

import kotlin.reflect.KType

/**
 * Declares the value type of caches that are accessed directly (via `cacheManager.getCache(name)`)
 * rather than through a `@Cacheable`/`@CachePut` annotation, whose return type would otherwise reveal
 * the type. [CacheFingerprintRegistry] folds these declarations in so that plain `getCache(name)`
 * resolves the same shape-fingerprinted physical cache everywhere, with no per-call-site wiring.
 *
 * Value types are [KType] (declared with `typeOf<T>()`) rather than `KClass`, so generic type
 * arguments are fingerprinted too — symmetric with the annotation path, which uses the method's
 * return `KType`. A module contributes one implementation for the direct-access caches it owns (its
 * value types may be module-private, so the declaration must live with the code that stores them).
 */
fun interface DirectAccessCacheTypeProvider {
  fun getDirectAccessCacheTypes(): Map<String, KType>
}
