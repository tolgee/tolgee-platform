package io.tolgee.service

import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.repository.ProjectRepository
import io.tolgee.repository.UserAccountRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Service
class PerformanceTestService {
    @Autowired
    private lateinit var userAccountRepository: UserAccountRepository

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Transactional
    fun createBulkTestData(userCount: Int, projectsPerUser: Int): Int {
        val users = (1..userCount).map { i ->
            UserAccount().apply {
                username = "perf_user_${UUID.randomUUID()}"
                name = "Performance Test User $i"
                role = UserAccount.Role.USER
            }
        }
        
        val savedUsers = userAccountRepository.saveAll(users)
        
        val projects = savedUsers.flatMap { user ->
            (1..projectsPerUser).map { i ->
                Project().apply {
                    name = "perf_project_${UUID.randomUUID()}"
                    userOwner = user
                }
            }
        }
        
        projectRepository.saveAll(projects)
        
        return userCount * projectsPerUser
    }

    @Transactional(readOnly = true)
    fun measureQueryPerformance(iterations: Int): Long {
        val start = System.currentTimeMillis()
        
        repeat(iterations) {
            projectRepository.findAll()
        }
        
        return System.currentTimeMillis() - start
    }

    fun measureParallelQueryPerformance(iterations: Int, threads: Int): Long {
        val executor = Executors.newFixedThreadPool(threads)
        val start = System.currentTimeMillis()
        
        val futures = (1..iterations).map {
            CompletableFuture.runAsync({
                projectRepository.findAll()
            }, executor)
        }
        
        CompletableFuture.allOf(*futures.toTypedArray()).join()
        executor.shutdown()
        
        return System.currentTimeMillis() - start
    }
} 