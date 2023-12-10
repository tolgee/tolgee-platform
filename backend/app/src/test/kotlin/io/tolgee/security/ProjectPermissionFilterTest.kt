package io.tolgee.security

import io.tolgee.AbstractServerAppAuthorizedControllerTest
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.generateUniqueString
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

@Transactional
class ProjectPermissionFilterTest : AbstractServerAppAuthorizedControllerTest() {
  @Test
  fun allowsAccessToPrivilegedUser() {
    val base = dbPopulator.createBase(generateUniqueString())
    performAuthGet("/v2/projects/${base.project.id}/translations").andIsOk
  }

  @Test
  fun deniesAccessToNonPrivilegedUser() {
    loginAsUserIfNotLogged()
    val base2 = dbPopulator.createBase(generateUniqueString(), "new-user")
    performAuthGet("/v2/projects/${base2.project.id}/translations").andIsNotFound
  }
}
