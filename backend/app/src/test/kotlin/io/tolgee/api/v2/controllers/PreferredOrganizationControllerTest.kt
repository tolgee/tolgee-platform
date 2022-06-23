package io.tolgee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.OrganizationTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PreferredOrganizationControllerTest : AuthorizedControllerTest() {

  lateinit var testData: OrganizationTestData

  @BeforeEach
  fun setup() {
    testData = OrganizationTestData()
    testDataService.saveTestData(testData.root)
  }

  @Test
  fun `returns correct preferred organization with base permission`() {
    userAccount = testData.franta
    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("name").isString.isEqualTo("test_username")
    }
  }

  @Test
  fun `returns correct preferred owning organization`() {
    userAccount = testData.pepa
    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("name").isString.isEqualTo("Organizacion")
    }
  }

  @Test
  fun `stores preferred organization`() {
    userAccount = testData.pepa
    performAuthGet("/v2/preferred-organization").andIsOk
    assertThat(userPreferencesService.find(userAccount!!.id)).isNotNull
  }
}
