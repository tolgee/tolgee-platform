package io.tolgee.cache

import io.tolgee.AbstractSpringTest
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.repository.PermissionRepository
import io.tolgee.repository.ProjectRepository
import io.tolgee.repository.UserAccountRepository
import io.tolgee.testing.assertions.Assertions
import org.mockito.Mockito
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cache.CacheManager
import org.testng.annotations.Test
import java.util.*

abstract class AbstractCacheTest : AbstractSpringTest() {
  @Autowired
  lateinit var cacheManager: CacheManager

  @Autowired
  @MockBean
  lateinit var userAccountRepository: UserAccountRepository

  @Autowired
  @MockBean
  override lateinit var projectRepository: ProjectRepository

  @Suppress("LateinitVarOverridesLateinitVar")
  @Autowired
  @MockBean
  lateinit var permissionRepository: PermissionRepository

  @Test
  fun `caches user account`() {
    val user = UserAccount().apply {
      name = "Account"
      id = 10
    }
    whenever(userAccountRepository.findById(user.id)).then { Optional.of(user) }
    userAccountService.getDto(user.id)
    Mockito.verify(userAccountRepository, times(1)).findById(user.id)
    userAccountService.getDto(user.id)
    Mockito.verify(userAccountRepository, times(1)).findById(user.id)
  }

  @Test
  fun `caches project`() {
    val project = Project(id = 1)
    whenever(projectRepository.findById(project.id)).then { Optional.of(project) }
    projectService.findDto(project.id)
    Mockito.verify(projectRepository, times(1)).findById(project.id)
    projectService.findDto(project.id)
    Mockito.verify(projectRepository, times(1)).findById(project.id)
  }

  @Test
  fun `caches permission`() {
    val permission = Permission(id = 1)
    whenever(permissionRepository.findOneByProjectIdAndUserId(1, 1)).then { permission }
    permissionService.findOneDtoByProjectIdAndUserId(1, 1)
    Mockito.verify(permissionRepository, times(1)).findOneByProjectIdAndUserId(1, 1)
    permissionService.findOneDtoByProjectIdAndUserId(1, 1)
    Mockito.verify(permissionRepository, times(1)).findOneByProjectIdAndUserId(1, 1)
  }

  @Test
  fun `is caching`() {
    cacheManager.getCache("cool cache")!!.put("test", "value")
    Assertions.assertThat(cacheManager.getCache("cool cache")!!.get("test")!!.get()).isEqualTo("value")
  }
}
