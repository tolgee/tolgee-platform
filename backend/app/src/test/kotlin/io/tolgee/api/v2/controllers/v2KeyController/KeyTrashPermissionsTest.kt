package io.tolgee.api.v2.controllers.v2KeyController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.KeySearchTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.Scope
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class KeyTrashPermissionsTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: KeySearchTestData

  @BeforeEach
  fun setup() {
    testData = KeySearchTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.projectBuilder.self }
  }

  // -- LIST TRASH (GET /keys/trash) requires KEYS_VIEW --

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_VIEW])
  @Test
  fun `list trash allowed with KEYS_VIEW scope`() {
    performProjectAuthGet("keys/trash").andIsOk
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.ACTIVITY_VIEW])
  @Test
  fun `list trash forbidden without KEYS_VIEW scope`() {
    performProjectAuthGet("keys/trash").andIsForbidden
  }

  // -- RESTORE (PUT /keys/trash/{keyId}/restore) requires KEYS_CREATE --

  @ProjectJWTAuthTestMethod
  @Test
  fun `restore allowed with full permissions`() {
    val key = keyService.get(testData.projectBuilder.self.id, "this-is-key", null)
    performProjectAuthDelete("keys/${key.id}").andIsOk

    performProjectAuthPut("keys/trash/${key.id}/restore").andIsOk
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_CREATE, Scope.KEYS_DELETE])
  @Test
  fun `restore allowed with KEYS_CREATE scope via API key`() {
    val key = keyService.get(testData.projectBuilder.self.id, "this-is-key", null)
    // Soft-delete first (KEYS_DELETE scope is included)
    performProjectAuthDelete("keys/${key.id}").andIsOk

    performProjectAuthPut("keys/trash/${key.id}/restore").andIsOk
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_VIEW, Scope.KEYS_DELETE])
  @Test
  fun `restore forbidden without KEYS_CREATE scope`() {
    val key = keyService.get(testData.projectBuilder.self.id, "this-is-key", null)
    performProjectAuthDelete("keys/${key.id}").andIsOk

    performProjectAuthPut("keys/trash/${key.id}/restore").andIsForbidden
  }

  // -- PERMANENTLY DELETE (DELETE /keys/trash/{keyId}) requires KEYS_DELETE --

  @ProjectJWTAuthTestMethod
  @Test
  fun `permanent delete allowed with full permissions`() {
    val key = keyService.get(testData.projectBuilder.self.id, "this-is-key", null)
    performProjectAuthDelete("keys/${key.id}").andIsOk

    performProjectAuthDelete("keys/trash/${key.id}").andIsOk
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_DELETE])
  @Test
  fun `permanent delete allowed with KEYS_DELETE scope via API key`() {
    val key = keyService.get(testData.projectBuilder.self.id, "this-is-key", null)
    performProjectAuthDelete("keys/${key.id}").andIsOk

    performProjectAuthDelete("keys/trash/${key.id}").andIsOk
  }

  // -- LIST DELETERS (GET /keys/trash/deleters) requires KEYS_VIEW --

  @ProjectJWTAuthTestMethod
  @Test
  fun `list deleters returns users who deleted keys`() {
    val key = keyService.get(testData.projectBuilder.self.id, "this-is-key", null)
    performProjectAuthDelete("keys/${key.id}").andIsOk

    performProjectAuthGet("keys/trash/deleters").andIsOk.andAssertThatJson {
      node("_embedded.users").isArray.isNotEmpty
      node("_embedded.users[0].id").isEqualTo(testData.user.id)
      node("_embedded.users[0].username").isEqualTo(testData.user.username)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_VIEW])
  @Test
  fun `list deleters allowed with KEYS_VIEW scope`() {
    performProjectAuthGet("keys/trash/deleters").andIsOk
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.ACTIVITY_VIEW])
  @Test
  fun `list deleters forbidden without KEYS_VIEW scope`() {
    performProjectAuthGet("keys/trash/deleters").andIsForbidden
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_VIEW, Scope.KEYS_CREATE])
  @Test
  fun `permanent delete forbidden without KEYS_DELETE scope`() {
    val key = keyService.get(testData.projectBuilder.self.id, "this-is-key", null)
    // Can't soft-delete without KEYS_DELETE, so do it via service
    executeInNewTransaction {
      keyService.softDeleteMultiple(listOf(key.id), deletedBy = testData.user)
    }

    performProjectAuthDelete("keys/trash/${key.id}").andIsForbidden
  }
}
