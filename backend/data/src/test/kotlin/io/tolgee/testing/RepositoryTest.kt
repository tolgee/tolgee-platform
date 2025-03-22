package io.tolgee.testing

import io.tolgee.configuration.TestContextConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource

@DataJpaTest
@ActiveProfiles("test")
@Import(TestContextConfiguration::class)
@TestPropertySource(properties = [
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false"
])
abstract class RepositoryTest {
    // Base class for repository tests that loads only the repository layer
    // This significantly reduces context loading time
} 