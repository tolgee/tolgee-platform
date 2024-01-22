package io.tolgee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.OrganizationTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserPreferencesControllerTest : AuthorizedControllerTest() {
  lateinit var testData: OrganizationTestData

  @BeforeEach
  fun setup() {
    testData = OrganizationTestData()
    testDataService.saveTestData(testData.root)
  }

  @Test
  fun `stores language`() {
    userAccount = testData.franta
    performAuthPut("/v2/user-preferences/set-language/de", null).andIsOk
    transactionTemplate.execute {
      assertThat(userAccountService.findActive(userAccount!!.username)?.preferences?.language).isEqualTo("de")
    }
  }

  @Test
  fun `returns preferences`() {
    userAccount = testData.pepa
    performAuthGet("/v2/user-preferences").andIsOk.andAssertThatJson {
      node("language").isEqualTo("de")
      node("preferredOrganizationId").isEqualTo(testData.pepaOrg.id)
    }
  }

  @Test
  fun `returns already stored preferences`() {
    userAccount = testData.jirina
    performAuthGet("/v2/user-preferences").andIsOk.andAssertThatJson {
      node("language").isEqualTo("ft")
      node("preferredOrganizationId").isEqualTo(testData.jirinaOrg.id)
    }
  }
}
