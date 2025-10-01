package io.tolgee.security

import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.UserAccount
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

@Transactional
class ServerAdminFilterTest : AuthorizedControllerTest() {
  @Test
  fun deniesAccessToRegularUser() {
    loginAsUserIfNotLogged()
    performAuthGet("/v2/administration/organizations").andIsForbidden
  }

  @Test
  fun allowsAccessToServerSupporter() {
    val serverAdmin =
      userAccountService.createUser(
        UserAccount(
          username = "serverSupporter",
          password = "admin",
          role = UserAccount.Role.SUPPORTER,
        ),
      )
    loginAsUser(serverAdmin)
    performAuthGet("/v2/administration/organizations").andIsOk
  }

  @Test
  fun allowsAccessToServerAdmin() {
    val serverAdmin =
      userAccountService.createUser(
        UserAccount(
          username = "serverAdmin",
          password = "admin",
          role = UserAccount.Role.ADMIN,
        ),
      )
    loginAsUser(serverAdmin)
    performAuthGet("/v2/administration/organizations").andIsOk
  }
}
