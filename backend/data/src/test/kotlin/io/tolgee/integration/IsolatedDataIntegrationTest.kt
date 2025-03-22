package io.tolgee.integration

import io.tolgee.testing.AbstractTestBase
import io.tolgee.testing.IsolatedTestDataBuilder
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

/**
 * Example test class that demonstrates how to use isolated test data.
 * Each test method creates and uses its own isolated data set.
 */
class IsolatedDataIntegrationTest : AbstractTestBase() {
    
    @Autowired
    private lateinit var testDataBuilder: IsolatedTestDataBuilder
    
    @Test
    @Transactional
    fun `test with isolated project data`() {
        // Create isolated test data for this test
        val testData = testDataBuilder.createCompleteProject(
            projectName = "Test Project 1",
            languages = listOf("en", "de"),
            keys = listOf("welcome", "goodbye")
        )
        
        // Use the test data
        assert(testData.project.name == "Test Project 1")
        assert(testData.languages.size == 2)
        assert(testData.keys.size == 2)
        assert(testData.translations.size == 4) // 2 languages * 2 keys
    }
    
    @Test
    @Transactional
    fun `another test with different isolated data`() {
        // Create different isolated test data for this test
        val testData = testDataBuilder.createCompleteProject(
            projectName = "Test Project 2",
            languages = listOf("en", "fr", "es"),
            keys = listOf("hello", "world")
        )
        
        // Use the test data
        assert(testData.project.name == "Test Project 2")
        assert(testData.languages.size == 3)
        assert(testData.keys.size == 2)
        assert(testData.translations.size == 6) // 3 languages * 2 keys
    }
    
    @Test
    @Transactional
    fun `test with content delivery config`() {
        // Create isolated test data with content delivery config
        val testData = testDataBuilder.createCompleteProject(
            projectName = "Test Project 3",
            withContentDelivery = true
        )
        
        // Use the test data
        assert(testData.project.name == "Test Project 3")
        assert(testData.contentDeliveryConfig != null)
        assert(testData.contentStorage != null)
    }
} 