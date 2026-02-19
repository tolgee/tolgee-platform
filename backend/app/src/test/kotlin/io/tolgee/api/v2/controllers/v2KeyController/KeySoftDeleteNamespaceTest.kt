package io.tolgee.api.v2.controllers.v2KeyController

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.NamespacesTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andGetContentAsString
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class KeySoftDeleteNamespaceTest : ProjectAuthControllerTest("/v2/projects/") {
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
  fun `hard-delete removes namespace when empty`() {
    val keyId = testData.singleKeyInNs2.id

    // Soft-delete the key
    performProjectAuthDelete("keys/$keyId", null).andIsOk

    // Namespace should still exist (soft-deleted key references it)
    namespaceService.find("ns-2", project.id).assert.isNotNull

    // Hard-delete the key via trash controller
    performProjectAuthDelete("keys/trash/$keyId", null).andIsOk

    // Now namespace should be deleted since no keys reference it
    namespaceService.find("ns-2", project.id).assert.isNull()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `namespace filter excludes namespaces with only soft-deleted keys`() {
    val keyId = testData.singleKeyInNs2.id

    // Before soft-delete: default, ns-1, ns-2 → 3 namespaces
    getUsedNamespaceNames().assert.contains("ns-2")

    // Soft-delete the only key in ns-2
    performProjectAuthDelete("keys/$keyId", null).andIsOk

    // After soft-delete: default, ns-1 → 2 namespaces (ns-2 excluded)
    val namesAfterDelete = getUsedNamespaceNames()
    namesAfterDelete.assert.contains("ns-1")
    namesAfterDelete.assert.doesNotContain("ns-2")
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `namespace filter includes namespace after key restore`() {
    val keyId = testData.singleKeyInNs2.id

    // Soft-delete the only key in ns-2
    performProjectAuthDelete("keys/$keyId", null).andIsOk

    // ns-2 should not be in used-namespaces
    getUsedNamespaceNames().assert.doesNotContain("ns-2")

    // Restore the key
    performProjectAuthPut("keys/trash/$keyId/restore", null).andIsOk

    // ns-2 should now be back in used-namespaces
    getUsedNamespaceNames().assert.contains("ns-2")
  }

  private fun getUsedNamespaceNames(): List<String?> {
    val json = performProjectAuthGet("used-namespaces").andIsOk.andGetContentAsString
    val tree = jacksonObjectMapper().readTree(json)
    return tree.path("_embedded").path("namespaces").map { it.path("name").textValue() }
  }
}
