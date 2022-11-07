package io.tolgee.api.v2.controllers

import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.NamespacesTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class NamespaceControllerTest : ProjectAuthControllerTest("/v2/projects/") {

  lateinit var testData: NamespacesTestData

  @BeforeEach
  fun createData() {
    testData = NamespacesTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns namespaces`() {
    performProjectAuthGet("namespaces").andIsOk.andAssertThatJson {
      node("_embedded.namespaces") {
        isArray.hasSize(2)
        node("[0]") {
          node("id").isValidId
          node("name").isEqualTo("ns-1")
        }
        node("[1]") {
          node("id").isValidId
          node("name").isEqualTo("ns-2")
        }
      }
    }
  }
}
