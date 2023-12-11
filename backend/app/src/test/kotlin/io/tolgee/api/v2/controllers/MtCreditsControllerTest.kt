package io.tolgee.api.v2.controllers

import io.tolgee.AbstractServerAppAuthorizedControllerTest
import io.tolgee.development.testDataBuilder.data.MtCreditsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc

@AutoConfigureMockMvc
class MtCreditsControllerTest : AbstractServerAppAuthorizedControllerTest() {
  lateinit var testData: MtCreditsTestData

  @BeforeEach
  fun setup() {
    testData = MtCreditsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  fun `returns project credit balance`() {
    performAuthGet("/v2/projects/${testData.organizationProject.id}/machine-translation-credit-balance")
      .andIsOk.andAssertThatJson {
        node("creditBalance").isEqualTo(120)
      }
  }

  @Test
  fun `returns organization credit balance`() {
    performAuthGet("/v2/organizations/${testData.organization.id}/machine-translation-credit-balance")
      .andIsOk.andAssertThatJson {
        node("creditBalance").isEqualTo(120)
      }
  }
}
