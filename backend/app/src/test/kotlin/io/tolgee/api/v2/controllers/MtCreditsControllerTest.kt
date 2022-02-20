package io.tolgee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.MtCreditsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class MtCreditsControllerTest : AuthorizedControllerTest() {
  lateinit var testData: MtCreditsTestData

  @BeforeEach
  fun setup() {
    testData = MtCreditsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  fun `returns user credit balance`() {
    performAuthGet("/v2/machine-translation-credit-balance").andIsOk.andAssertThatJson {
      node("creditBalance").isEqualTo(15000)
    }
  }

  @Test
  fun `returns user project credit balance`() {
    performAuthGet("/v2/projects/${testData.projectBuilder.self.id}/machine-translation-credit-balance")
      .andIsOk.andAssertThatJson {
        node("creditBalance").isEqualTo(15000)
      }
  }

  @Test
  fun `returns organization project credit balance`() {
    performAuthGet("/v2/projects/${testData.organizationProject.id}/machine-translation-credit-balance")
      .andIsOk.andAssertThatJson {
        node("creditBalance").isEqualTo(12000)
      }
  }

  @Test
  fun `returns organization credit balance`() {
    performAuthGet("/v2/organizations/${testData.organization.id}/machine-translation-credit-balance")
      .andIsOk.andAssertThatJson {
        node("creditBalance").isEqualTo(12000)
      }
  }
}
