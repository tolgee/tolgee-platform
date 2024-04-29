package io.tolgee.api.v2.controllers.v2ProjectsController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.ProjectTransferringTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class ProjectsControllerTransferringTest : ProjectAuthControllerTest("/v2/projects/") {
  @Test
  @ProjectJWTAuthTestMethod
  fun `doesn't transfer to organization when not permitted`() {
    val testData = ProjectTransferringTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user2
    projectSupplier = { testData.project }
    performProjectAuthPut("/transfer-to-organization/${testData.organization.id}", null).andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `provides transfer options`() {
    val testData = ProjectTransferringTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.project }
    performProjectAuthGet("/transfer-options").andPrettyPrint.andIsOk.andAssertThatJson {
      node("_embedded.transferOptions") {
        isArray
        node("[0].name").isEqualTo("Another organization")
      }
    }
  }
}
