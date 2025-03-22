package io.tolgee.example

import io.tolgee.AbstractDatabaseTest
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.repository.ProjectRepository
import io.tolgee.repository.UserAccountRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import java.util.*

/**
 * Example test class demonstrating the use of the optimized test infrastructure.
 * This test loads only the repository layer, significantly reducing context loading time.
 */
class OptimizedRepositoryTest : AbstractDatabaseTest() {
    
    @Autowired
    private lateinit var userAccountRepository: UserAccountRepository
    
    @Autowired
    private lateinit var projectRepository: ProjectRepository
    
    @Test
    fun `test creates and retrieves user with unique data`() {
        // Create user with unique data
        val uniqueUsername = "user_${getUniqueString()}"
        val user = UserAccount().apply {
            username = uniqueUsername
            name = "Test User ${getUniqueString()}"
            role = UserAccount.Role.USER
        }

        // Save user
        val savedUser = userAccountRepository.save(user)
        entityManager.flush()
        entityManager.clear()

        // Retrieve user
        val retrievedUser = userAccountRepository.findById(savedUser.id).orElse(null)
        assertNotNull(retrievedUser)
        assertEquals(uniqueUsername, retrievedUser.username)
    }

    @Test
    fun `test creates and retrieves project with unique data`() {
        // Create user with unique data
        val uniqueUsername = "user_${getUniqueString()}"
        val user = UserAccount().apply {
            username = uniqueUsername
            name = "Test User ${getUniqueString()}"
            role = UserAccount.Role.USER
        }
        val savedUser = userAccountRepository.save(user)

        // Create project with unique data
        val uniqueProjectName = "project_${getUniqueString()}"
        val project = Project().apply {
            name = uniqueProjectName
            userOwner = savedUser
        }

        // Save project
        val savedProject = projectRepository.save(project)
        entityManager.flush()
        entityManager.clear()

        // Retrieve project
        val retrievedProject = projectRepository.findById(savedProject.id).orElse(null)
        assertNotNull(retrievedProject)
        assertEquals(uniqueProjectName, retrievedProject.name)
    }
} 