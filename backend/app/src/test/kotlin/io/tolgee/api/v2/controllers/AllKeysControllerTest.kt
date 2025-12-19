package io.tolgee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.KeysTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class AllKeysControllerTest : ProjectAuthControllerTest("/v2/projects/") {
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
  fun `returns default branch keys sorted`() {
    performProjectAuthGet("all-keys").andPrettyPrint.andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(3)
        node("[0]") {
          node("id").isValidId
          node("namespace").isNull()
          node("name").isEqualTo("first_key")
          node("branch").isNull()
        }
        node("[1]") {
          node("name").isEqualTo("second_key")
          node("branch").isNull()
        }
        node("[2]") {
          node("name").isEqualTo("key_with_referecnces")
          node("branch").isNull()
        }
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns branch keys when branch param provided`() {
    performProjectAuthGet("all-keys?branch=dev").andPrettyPrint.andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(1)
        node("[0]") {
          node("id").isValidId
          node("name").isEqualTo("first_key")
          node("branch").isEqualTo("dev")
        }
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `fails when branch does not exist`() {
    performProjectAuthGet("all-keys?branch=does-not-exist")
      .andPrettyPrint
      .andIsBadRequest
      .andHasErrorMessage(Message.BRANCH_NOT_FOUND)
  }
}
