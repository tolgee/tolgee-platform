package io.tolgee.testing.cache

import io.tolgee.component.cache.CacheFingerprintRegistry
import io.tolgee.testing.assert
import java.lang.reflect.Modifier

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
