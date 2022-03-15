package io.tolgee.security

import io.tolgee.fixtures.generateUniqueString
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional

@Transactional
class ProjectPermissionFilterTest : AuthorizedControllerTest() {

  @field:Autowired
  lateinit var projectHolder: ProjectHolder

  @Test
  fun allowsAccessToPrivilegedUser() {
    val base = dbPopulator.createBase(generateUniqueString())
    performAuthGet("/api/project/${base.id}/translations/en")
      .andExpect(MockMvcResultMatchers.status().isOk).andReturn()
  }

  @Test
  fun deniesAccessToNonPrivilegedUser() {
    val base2 = dbPopulator.createBase(generateUniqueString(), "newUser")
    performAuthGet("/api/project/${base2.id}/translations/en")
      .andExpect(MockMvcResultMatchers.status().isForbidden).andReturn()
  }

  @Test
  fun returnsNotFoundWhenProjectNotExists() {
    val user = dbPopulator.createUserIfNotExists("newUser")
    performAuthGet("/api/project/${user.id}/translations/en")
      .andExpect(MockMvcResultMatchers.status().isNotFound).andReturn()
  }
}
