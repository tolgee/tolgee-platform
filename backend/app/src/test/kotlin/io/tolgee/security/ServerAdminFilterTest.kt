package io.tolgee.security

import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.model.UserAccount
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

@Transactional
class ServerAdminFilterTest : AuthorizedControllerTest() {
  @Test
  fun deniesAccessToRegularAdmin() {
    dbPopulator.createBase(generateUniqueString(), "admin")
    performAuthGet("/v2/administration/organizations").andIsForbidden
  }

  @Test
  fun allowsAccessToServerAdmin() {
    dbPopulator.createBase(generateUniqueString(), "admin")
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
