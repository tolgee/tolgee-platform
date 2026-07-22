package io.tolgee.cache

import io.tolgee.AbstractSpringTest
import io.tolgee.component.cache.CacheFingerprintRegistry
import io.tolgee.constants.Caches
import io.tolgee.testing.cache.assertAllConstantsFingerprinted
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class CacheFingerprintCoverageTest : AbstractSpringTest() {
  @Autowired
  lateinit var cacheFingerprintRegistry: CacheFingerprintRegistry

  @Test
  fun `every cache constant is shape-fingerprinted`() {
    cacheFingerprintRegistry.assertAllConstantsFingerprinted(Caches::class.java)
  }
}
