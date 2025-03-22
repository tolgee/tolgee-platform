package io.tolgee.testing

import io.tolgee.configuration.TestSliceConfiguration
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional

@ExtendWith(SpringExtension::class)
@DataJpaTest
@Import(TestSliceConfiguration::class)
@ActiveProfiles("test")
@Transactional
abstract class RepositoryTestBase {
    // Base class for repository tests that use a sliced Spring context
    // This significantly reduces context load time for repository tests
} 