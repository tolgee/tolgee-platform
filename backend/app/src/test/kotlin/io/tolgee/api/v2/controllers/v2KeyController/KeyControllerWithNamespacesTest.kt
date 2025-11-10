package io.tolgee.api.v2.controllers.v2KeyController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.NamespacesTestData
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.dtos.request.key.EditKeyDto
import io.tolgee.fixtures.andAssertError
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
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
class KeyControllerWithNamespacesTest : ProjectAuthControllerTest("/v2/projects/") {
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
    enableNamespaces()
    performProjectAuthPost("keys", mapOf("name" to "super_key", "namespace" to "new_ns"))
      .andIsCreated
      .andAssertThatJson {
        node("name").isEqualTo("super_key")
        node("namespace").isEqualTo("new_ns")
      }
    keyService.get(project.id, "super_key", "new_ns").assert.isNotNull
    namespaceService.find("new_ns", project.id).assert.isNotNull
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `blank namespace doesn't create ns`() {
    enableNamespaces()
    performProjectAuthPost("keys", CreateKeyDto(name = "super_key", namespace = ""))
      .andIsCreated
    namespaceService.find("", project.id).assert.isNull()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `creates key in existing namespace`() {
    enableNamespaces()
    performProjectAuthPost("keys", CreateKeyDto(name = "super_key", namespace = "ns-1"))
      .andIsCreated
      .andAssertThatJson {
        node("name").isEqualTo("super_key")
        node("namespace").isEqualTo("ns-1")
      }
    keyService.get(testData.projectBuilder.self.id, "super_key", "ns-1").assert.isNotNull
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not create key when not unique in ns`() {
    enableNamespaces()
    performProjectAuthPost("keys", CreateKeyDto(name = "key", "ns-1"))
      .andAssertError
      .isCustomValidation
      .hasMessage("key_exists")
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `updates key in ns`() {
    enableNamespaces()
    performProjectAuthPut("keys/${testData.keyInNs1.id}", EditKeyDto(name = "super_k", "ns-2"))
      .andIsOk
      .andAssertThatJson {
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
      .andIsOk
      .andAssertThatJson {
        node("namespace").isNull()
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `blank namespace doesn't create ns on update`() {
    performProjectAuthPut("keys/${testData.keyInNs1.id}", EditKeyDto(name = "super_k", ""))
      .andIsOk
      .andAssertThatJson {
        node("namespace").isNull()
      }
    namespaceService.find("", project.id).assert.isNull()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `throws error when moving key to default ns where a key with same name already exists`() {
    enableNamespaces()
    val keyName = "super_ultra_cool_key"
    val namespace = "super_ultra_cool_namespace"

    performProjectAuthPost("keys", CreateKeyDto(name = keyName))
      .andIsCreated
    performProjectAuthPost("keys", CreateKeyDto(name = keyName, namespace = namespace))
      .andIsCreated

    val keyId = keyService.get(project.id, keyName, namespace).id

    performProjectAuthPut("keys/$keyId/complex-update", mapOf("name" to keyName, "namespace" to ""))
      .andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage("key_exists")

    performProjectAuthPut("keys/$keyId/complex-update", mapOf("name" to keyName, "namespace" to null))
      .andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage("key_exists")

    performProjectAuthPut("keys/$keyId/complex-update", mapOf("name" to keyName))
      .andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage("key_exists")
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not create key when exists empty ns`() {
    performProjectAuthPost("keys", CreateKeyDto(name = "key2", namespace = ""))
      .andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage("key_exists")
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `deletes ns when empty`() {
    performProjectAuthDelete("keys/${testData.singleKeyInNs2.id}").andIsOk
    keyService.find(project.id, "super_k", "ns-2").assert.isNull()
    namespaceService.find("ns-2", project.id).assert.isNull()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `deletes ns when empty on update`() {
    enableNamespaces()
    performProjectAuthPut("keys/${testData.singleKeyInNs2.id}", EditKeyDto(name = "super_k", "ns-1"))
      .andIsOk
    namespaceService.find("ns-2", project.id).assert.isNull()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `deletes all keys`() {
    val ids =
      testData.projectBuilder.data.keys
        .map { it.self.id }
    performProjectAuthDelete("keys/${ids.joinToString(",")}").andIsOk
    executeInNewTransaction {
      ids.forEach { keyService.find(it).assert.isNull() }
    }

    namespaceService.getAllInProject(testData.projectBuilder.self.id).assert.isEmpty()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `key with namespace cannot be created when useNamespaces feature is disabled`() {
    performProjectAuthPost("keys", mapOf("name" to "super_key", "namespace" to ""))
      .andIsCreated
      .andAssertThatJson {
        node("namespace").isNull()
      }

    performProjectAuthPost("keys", mapOf("name" to "super_key", "namespace" to "new_ns"))
      .andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage("namespace_cannot_be_used_when_feature_is_disabled")
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `key with namespace cannot be edited when useNamespaces feature is disabled`() {
    performProjectAuthPut("keys/${testData.keyWithoutNs.id}", EditKeyDto(name = "super_k", ""))
      .andIsOk
      .andAssertThatJson {
        node("namespace").isNull()
      }

    performProjectAuthPut("keys/${testData.keyWithoutNs.id}", EditKeyDto(name = "super_k", "ns-2"))
      .andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage("namespace_cannot_be_used_when_feature_is_disabled")
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `key with namespace cannot be complex-edited when useNamespaces feature is disabled`() {
    performProjectAuthPut(
      "keys/${testData.keyWithoutNs.id}/complex-update",
      mapOf("name" to "new-name", "namespace" to ""),
    ).andIsOk.andAssertThatJson {
      node("namespace").isNull()
    }

    performProjectAuthPut(
      "keys/${testData.keyWithoutNs.id}/complex-update",
      mapOf("name" to "new-name", "namespace" to "ns-2"),
    ).andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage("namespace_cannot_be_used_when_feature_is_disabled")
  }

  private fun enableNamespaces() {
    val projectFetched = projectService.get(project.id)
    projectFetched.useNamespaces = true
    projectService.save(projectFetched)
  }
}
