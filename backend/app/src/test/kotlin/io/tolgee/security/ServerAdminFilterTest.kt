package io.tolgee.security

import io.tolgee.AbstractServerAppAuthorizedControllerTest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.UserAccount
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

@Transactional
class ServerAdminFilterTest : AbstractServerAppAuthorizedControllerTest() {
  @Test
  fun deniesAccessToRegularUser() {
    loginAsUserIfNotLogged()
    performAuthGet("/v2/administration/organizations").andIsForbidden
  }

  @Test
  fun allowsAccessToServerAdmin() {
    val serverAdmin = userAccountService.createUser(
      UserAccount(
        username = "serverAdmin",
        password = "admin",
        role = UserAccount.Role.ADMIN
      )
    )
    loginAsUser(serverAdmin)
    performAuthGet("/v2/administration/organizations").andIsOk
  }
}
