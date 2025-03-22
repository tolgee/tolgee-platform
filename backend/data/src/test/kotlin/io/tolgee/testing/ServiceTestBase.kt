package io.tolgee.testing

import io.tolgee.configuration.TestSliceConfiguration
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [TestSliceConfiguration::class])
@ActiveProfiles("test")
@Transactional
abstract class ServiceTestBase {
    // Base class for service tests with a reduced context
    // This provides a middle ground between full integration tests and repository tests
} 