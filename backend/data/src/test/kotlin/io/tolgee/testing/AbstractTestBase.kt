package io.tolgee.testing

import io.tolgee.configuration.TestApplicationConfig
import io.tolgee.model.Project
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import io.tolgee.model.contentDelivery.ContentStorage
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.concurrent.atomic.AtomicLong

/**
 * Base class for all integration tests.
 * Uses optimized test configuration to reduce context loading time.
 * Provides utilities for test data isolation to avoid conflicts between tests.
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [TestApplicationConfig::class],
    properties = ["spring.main.allow-bean-definition-overriding=true"]
)
@ActiveProfiles("test")
abstract class AbstractTestBase {
    
    companion object {
        private val UNIQUE_ID = AtomicLong(System.currentTimeMillis())
    }
    
    /**
     * Generates a unique string identifier for test data.
     * This ensures that each test uses unique data to avoid conflicts.
     */
    protected fun getUniqueString(prefix: String = ""): String {
        return "${prefix}_${UNIQUE_ID.incrementAndGet()}"
    }
    
    /**
     * Creates a test project with a unique name to avoid conflicts.
     */
    protected fun createUniqueProject(name: String? = null): Project {
        val projectName = name ?: getUniqueString("project")
        val project = Project()
        project.name = projectName
        project.slug = getUniqueString("slug")
        return project
    }
    
    /**
     * Creates a unique content delivery config for testing.
     */
    protected fun createUniqueContentDeliveryConfig(project: Project): ContentDeliveryConfig {
        val config = ContentDeliveryConfig(project)
        config.name = getUniqueString("config")
        config.slug = getUniqueString("slug")
        return config
    }
    
    /**
     * Creates a unique content storage for testing.
     */
    protected fun createUniqueContentStorage(project: Project): ContentStorage {
        val storage = ContentStorage()
        storage.project = project
        storage.name = getUniqueString("storage")
        return storage
    }
    
    /**
     * Helper method to create a unique test data set.
     * This can be extended with more entity types as needed.
     */
    protected fun createUniqueTestData(): TestDataSet {
        val project = createUniqueProject()
        val config = createUniqueContentDeliveryConfig(project)
        val storage = createUniqueContentStorage(project)
        
        return TestDataSet(
            project = project,
            contentDeliveryConfig = config,
            contentStorage = storage
        )
    }
    
    /**
     * Data class to hold a set of related test entities.
     */
    data class TestDataSet(
        val project: Project,
        val contentDeliveryConfig: ContentDeliveryConfig,
        val contentStorage: ContentStorage
    )
} 