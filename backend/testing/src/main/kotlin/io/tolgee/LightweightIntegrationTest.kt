package io.tolgee

import io.tolgee.configuration.LightweightTestConfig
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(classes = [LightweightTestConfig::class])
@ActiveProfiles("test")
@Transactional
abstract class LightweightIntegrationTest : AbstractTransactionalTest() {
    // This base class uses a minimal context for faster loading
    // Use this for tests that don't need the full application context
} 