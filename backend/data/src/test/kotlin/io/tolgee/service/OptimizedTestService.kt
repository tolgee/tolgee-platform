package io.tolgee.service

import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.repository.ProjectRepository
import io.tolgee.repository.UserAccountRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OptimizedTestService {
    @Autowired
    private lateinit var userAccountRepository: UserAccountRepository

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Transactional
    fun createUniqueTestData(prefix: String): Project {
        val user = UserAccount().apply {
            username = "${prefix}_user_${UUID.randomUUID()}"
            name = "Test User"
            role = UserAccount.Role.USER
        }
        val savedUser = userAccountRepository.save(user)
        
        val project = Project().apply {
            name = "${prefix}_project_${UUID.randomUUID()}"
            userOwner = savedUser
        }
        return projectRepository.save(project)
    }

    @Transactional(readOnly = true)
    fun findProjectById(id: Long): Project? {
        return projectRepository.findById(id).orElse(null)
    }
} 