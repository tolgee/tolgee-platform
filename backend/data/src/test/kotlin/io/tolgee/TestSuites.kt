package io.tolgee

import io.tolgee.example.OptimizedIntegrationTest
import io.tolgee.example.OptimizedRepositoryTest
import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite
import org.junit.platform.suite.api.SuiteDisplayName

/**
 * Fast test suite that includes only lightweight tests.
 * Use this for quick feedback during development.
 */
@Suite
@SuiteDisplayName("Fast Tests")
@SelectClasses(
    OptimizedRepositoryTest::class,
    OptimizedIntegrationTest::class
    // Add more lightweight test classes here
)
class FastTestSuite

/**
 * Repository test suite that includes only repository tests.
 */
@Suite
@SuiteDisplayName("Repository Tests")
@SelectClasses(
    OptimizedRepositoryTest::class
    // Add more repository test classes here
)
class RepositoryTestSuite

/**
 * Integration test suite that includes lightweight integration tests.
 */
@Suite
@SuiteDisplayName("Integration Tests")
@SelectClasses(
    OptimizedIntegrationTest::class
    // Add more integration test classes here
)
class IntegrationTestSuite 