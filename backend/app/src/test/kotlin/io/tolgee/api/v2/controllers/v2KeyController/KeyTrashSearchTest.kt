package io.tolgee.api.v2.controllers.v2KeyController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.KeySearchTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class KeyTrashSearchTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: KeySearchTestData

  @BeforeEach
  fun setup() {
    testData = KeySearchTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.projectBuilder.self }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `search in trash finds soft-deleted keys`() {
    val key = keyService.get(testData.projectBuilder.self.id, "this-is-key", null)
    performProjectAuthDelete("keys/${key.id}").andIsOk

    performProjectAuthGet("keys/trash?search=key").andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(1)
        node("[0].name").isEqualTo("this-is-key")
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `search in trash does not return active keys`() {
    val key = keyService.get(testData.projectBuilder.self.id, "this-is-key", null)
    performProjectAuthDelete("keys/${key.id}").andIsOk

    // "beauty" is an active key — should not appear in trash search
    performProjectAuthGet("keys/trash?search=beauty").andIsOk.andAssertThatJson {
      node("_embedded").isAbsent()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `list trash without search returns soft-deleted keys`() {
    val key1 = keyService.get(testData.projectBuilder.self.id, "this-is-key", null)
    val key2 = keyService.get(testData.projectBuilder.self.id, "this-is-key-2", null)
    performProjectAuthDelete("keys/${key1.id}").andIsOk
    performProjectAuthDelete("keys/${key2.id}").andIsOk

    performProjectAuthGet("keys/trash").andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(2)
      }
      node("page.totalElements").isEqualTo(2)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `trash is empty when no keys are soft-deleted`() {
    performProjectAuthGet("keys/trash").andIsOk.andAssertThatJson {
      node("page.totalElements").isEqualTo(0)
    }
  }
}
