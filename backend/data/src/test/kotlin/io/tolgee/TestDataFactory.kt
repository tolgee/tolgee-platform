package io.tolgee

import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.repository.ProjectRepository
import io.tolgee.repository.UserAccountRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.atomic.AtomicLong

@Component
class TestDataFactory {
    @Autowired
    private lateinit var userAccountRepository: UserAccountRepository

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    private val userIdCounter = AtomicLong(1000)
    private val projectIdCounter = AtomicLong(1000)

    fun createUser(
        username: String = "user_${UUID.randomUUID()}",
        name: String = "Test User",
        role: UserAccount.Role = UserAccount.Role.USER
    ): UserAccount {
        val user = UserAccount().apply {
            this.username = username
            this.name = name
            this.role = role
        }
        return userAccountRepository.save(user)
    }

    fun createProject(
        name: String = "project_${UUID.randomUUID()}",
        owner: UserAccount? = null
    ): Project {
        val userOwner = owner ?: createUser()
        val project = Project().apply {
            this.name = name
            this.userOwner = userOwner
        }
        return projectRepository.save(project)
    }
} 