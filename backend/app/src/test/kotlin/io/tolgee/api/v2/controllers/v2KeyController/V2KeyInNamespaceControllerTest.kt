package io.tolgee.api.v2.controllers.v2KeyController

import io.tolgee.constants.Message
import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.NamespacesTestData
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.dtos.request.key.EditKeyDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.isValidId
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.Resource

@SpringBootTest
@AutoConfigureMockMvc
class V2KeyInNamespaceControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  @Value("classpath:screenshot.png")
  lateinit var screenshotFile: Resource

  lateinit var testData: NamespacesTestData

  @BeforeEach
  fun setup() {
    testData = NamespacesTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.projectBuilder.self }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `creates key and namespace`() {
    performProjectAuthPost("keys", CreateKeyDto(name = "super_key", namespace = "new_ns"))
      .andIsCreated.andPrettyPrint.andAssertThatJson {
        node("name").isEqualTo("super_key")
        node("namespace").isEqualTo("new_ns")
      }
    keyService.get(project.id, "super_key", "new_ns").assert.isNotNull
    namespaceService.find("new_ns", project.id).assert.isNotNull
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `blank namespace doesn't create ns`() {
    performProjectAuthPost("keys", CreateKeyDto(name = "super_key", namespace = ""))
      .andIsCreated
    namespaceService.find("", project.id).assert.isNull()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `creates key in existing namespace`() {
    performProjectAuthPost("keys", CreateKeyDto(name = "super_key", namespace = "ns-1"))
      .andIsCreated.andPrettyPrint.andAssertThatJson {
        node("name").isEqualTo("super_key")
        node("namespace").isEqualTo("ns-1")
      }
    keyService.get(testData.projectBuilder.self.id, "super_key", "ns-1").assert.isNotNull
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not create key when not unique in ns`() {
    performProjectAuthPost("keys", CreateKeyDto(name = "key", "ns-1"))
      .andIsBadRequest.andHasErrorMessage(Message.KEY_EXISTS)
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `updates key in ns`() {
    performProjectAuthPut("keys/${testData.keyInNs1.id}", EditKeyDto(name = "super_k", "ns-2"))
      .andIsOk.andPrettyPrint.andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo("super_k")
        node("namespace").isEqualTo("ns-2")
      }
    keyService.get(project.id, "super_k", "ns-2").assert.isNotNull
    namespaceService.find("ns-2", project.id).assert.isNotNull
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `changes namespace to default`() {
    performProjectAuthPut("keys/${testData.keyInNs1.id}", EditKeyDto(name = "super_k", null))
      .andIsOk.andPrettyPrint.andAssertThatJson {
        node("namespace").isNull()
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `blank namespace doesn't create ns on update`() {
    performProjectAuthPut("keys/${testData.keyInNs1.id}", EditKeyDto(name = "super_k", ""))
      .andIsOk.andPrettyPrint.andAssertThatJson {
        node("namespace").isNull()
      }
    namespaceService.find("", project.id).assert.isNull()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `deletes ns when empty`() {
    performProjectAuthDelete("keys/${testData.singleKeyInNs2.id}").andIsOk
    keyService.find(project.id, "super_k", "ns-2").assert.isNull()
    namespaceService.find("ns-2", project.id).assert.isNull()
  }
}
