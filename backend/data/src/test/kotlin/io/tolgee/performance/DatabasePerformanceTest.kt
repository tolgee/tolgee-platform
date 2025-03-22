package io.tolgee.performance

import io.tolgee.AbstractDatabaseTest
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.repository.ProjectRepository
import io.tolgee.repository.UserAccountRepository
import io.tolgee.testing.assertions.TestAssertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
@Tag("performance")
class DatabasePerformanceTest : AbstractDatabaseTest() {

    @Autowired
    private lateinit var userAccountRepository: UserAccountRepository

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Test
    fun `test database insert performance`() {
        TestAssertions.assertExecutionTime(1000) {
            // Create 100 users with unique data
            val users = (1..100).map { i ->
                UserAccount().apply {
                    username = "perf_user_${UUID.randomUUID()}"
                    name = "Performance Test User $i"
                    role = UserAccount.Role.USER
                }
            }
            
            userAccountRepository.saveAll(users)
            entityManager.flush()
        }
    }

    @Test
    fun `test database query performance`() {
        // Create test data
        val user = UserAccount().apply {
            username = "perf_user_${UUID.randomUUID()}"
            name = "Performance Test User"
            role = UserAccount.Role.USER
        }
        val savedUser = userAccountRepository.save(user)
        
        // Create 100 projects
        val projects = (1..100).map { i ->
            Project().apply {
                name = "perf_project_${UUID.randomUUID()}"
                userOwner = savedUser
            }
        }
        projectRepository.saveAll(projects)
        entityManager.flush()
        entityManager.clear()
        
        // Test query performance
        TestAssertions.assertExecutionTime(500) {
            projectRepository.findAll()
        }
    }
} 