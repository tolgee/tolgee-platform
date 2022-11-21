package io.tolgee.api.v2.controllers

import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.KeysTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.Resource

@SpringBootTest
@AutoConfigureMockMvc
class V2AllKeysControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  companion object {
    val MAX_OK_NAME = (1..2000).joinToString("") { "a" }
    val LONGER_NAME = (1..2001).joinToString("") { "a" }
  }

  @Value("classpath:screenshot.png")
  lateinit var screenshotFile: Resource

  lateinit var testData: KeysTestData

  @BeforeEach
  fun setup() {
    testData = KeysTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.project }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns all keys sorted`() {
    performProjectAuthGet("all-keys").andPrettyPrint.andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(3)
        node("[0]") {
          node("id").isValidId
          node("namespace").isNull()
          node("name").isEqualTo("first_key")
        }
      }
    }
  }
}
