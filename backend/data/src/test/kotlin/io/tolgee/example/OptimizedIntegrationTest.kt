package io.tolgee.example

import io.tolgee.LightweightIntegrationTest
import io.tolgee.model.Project
import io.tolgee.repository.ProjectRepository
import io.tolgee.service.project.ProjectService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

/**
 * Example test class demonstrating the use of the lightweight integration test infrastructure.
 * This test loads a minimal application context, significantly reducing context loading time.
 */
class OptimizedIntegrationTest : LightweightIntegrationTest() {
    
    @Autowired
    private lateinit var projectService: ProjectService
    
    @Autowired
    private lateinit var projectRepository: ProjectRepository
    
    @Test
    fun `test project service operations`() {
        // Create unique test data
        val projectName = "Test Project ${UUID.randomUUID()}"
        
        // Use service to create entity
        val project = Project().apply {
            name = projectName
            organizationOwner = null
        }
        projectRepository.save(project)
        
        // Use service to retrieve entity
        val retrieved = projectService.get(project.id)
        assertEquals(projectName, retrieved.name)
    }
} 