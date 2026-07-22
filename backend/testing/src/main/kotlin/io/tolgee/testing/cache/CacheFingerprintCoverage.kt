package io.tolgee.testing.cache

import io.tolgee.component.cache.CacheFingerprintRegistry
import io.tolgee.testing.assert
import java.lang.reflect.Modifier

/**
 * Asserts that every `String` constant on [constantsClass] resolves to a shape-fingerprinted physical
 * cache name — a fail-loud guard so a forgotten `DirectAccessCacheTypeProvider`/`@Cacheable`
 * declaration, or a name that would silently opt out by containing the separator, breaks the build.
 */
fun CacheFingerprintRegistry.assertAllConstantsFingerprinted(constantsClass: Class<*>) {
  val cacheNames =
    constantsClass.fields
      .filter { Modifier.isStatic(it.modifiers) && it.type == String::class.java }
      .map { it.get(null) as String }

  cacheNames
    .filter { it.contains(CacheFingerprintRegistry.SEPARATOR) }
    .assert
    .describedAs(
      "a logical cache name must not contain the physical-name separator '%s'",
      CacheFingerprintRegistry.SEPARATOR,
    ).isEmpty()

  cacheNames
    .filter { physicalName(it) == it }
    .assert
    .describedAs(
      "these caches are not shape-fingerprinted; declare each value type via a " +
        "DirectAccessCacheTypeProvider or a @Cacheable return type",
    ).isEmpty()
}
