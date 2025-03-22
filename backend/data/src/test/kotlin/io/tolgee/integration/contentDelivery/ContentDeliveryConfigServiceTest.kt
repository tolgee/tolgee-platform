package io.tolgee.integration.contentDelivery

import io.tolgee.testing.AbstractTestBase
import io.tolgee.testing.IsolatedTestDataBuilder
import io.tolgee.service.contentDelivery.ContentDeliveryConfigService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class ContentDeliveryConfigServiceTest : AbstractTestBase() {
    
    @Autowired
    private lateinit var contentDeliveryConfigService: ContentDeliveryConfigService
    
    @Autowired
    private lateinit var testDataBuilder: IsolatedTestDataBuilder
    
    @Test
    @Transactional
    fun `test optimized methods`() {
        // Create test data
        val testData = testDataBuilder.createCompleteProject(
            withContentDelivery = true
        )
        
        // Test optimized get method
        val config = contentDeliveryConfigService.getOptimized(
            testData.project.id,
            testData.contentDeliveryConfig!!.id
        )
        assertEquals(testData.contentDeliveryConfig!!.id, config.id)
        
        // Test optimized exists method
        val exists = contentDeliveryConfigService.existsOptimized(
            testData.project.id,
            testData.contentDeliveryConfig!!.id
        )
        assertTrue(exists)
        
        // Test optimized count method
        val count = contentDeliveryConfigService.countByProjectIdOptimized(testData.project.id)
        assertEquals(1, count)
        
        // Test optimized get all method
        val configs = contentDeliveryConfigService.getAllForProjectOptimized(testData.project.id)
        assertEquals(1, configs.size)
    }
} 