package io.tolgee.security

import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

@Transactional
class ProjectPermissionFilterTest : AuthorizedControllerTest() {
  @Test
  fun allowsAccessToPrivilegedUser() {
    val base = dbPopulator.createBase()
    performAuthGet("/v2/projects/${base.project.id}/translations").andIsOk
  }

  @Test
  fun deniesAccessToNonPrivilegedUser() {
    loginAsUserIfNotLogged()
    val base2 = dbPopulator.createBase("new-user")
    performAuthGet("/v2/projects/${base2.project.id}/translations").andIsNotFound
  }
}
