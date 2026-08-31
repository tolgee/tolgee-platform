package io.tolgee.security

import io.tolgee.development.testDataBuilder.data.ProjectApiKeyAdminRightsTestData
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.testing.AbstractControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc

/**
 * A project API key is bounded by the user's real project membership. Server-admin and supporter reach belongs to the
 * user acting directly; letting a key inherit it would silently widen that key to every project on the server.
 */
@AutoConfigureMockMvc
class ProjectApiKeyAdminRightsTest : AbstractControllerTest() {
  private lateinit var testData: ProjectApiKeyAdminRightsTestData

  @BeforeEach
  fun setup() {
    testData = ProjectApiKeyAdminRightsTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `an admin's key cannot read a project the admin never joined`() {
    performGet("$translationsUrl?ak=${key(testData.outsideAdmin, Scope.TRANSLATIONS_VIEW)}").andIsNotFound
  }

  @Test
  fun `a supporter's key cannot read a project the supporter never joined`() {
    performGet("$translationsUrl?ak=${key(testData.outsideSupporter, Scope.TRANSLATIONS_VIEW)}").andIsNotFound
  }

  @Test
  fun `an admin's key works within the admin's real membership`() {
    performGet("$translationsUrl?ak=${key(testData.memberAdmin, Scope.TRANSLATIONS_VIEW)}").andIsOk
  }

  @Test
  fun `an admin's key cannot exceed the admin's real membership`() {
    performPut(
      "$translationsUrl?ak=${key(testData.memberAdmin, Scope.TRANSLATIONS_EDIT)}",
      SetTranslationsWithKeyDto(
        key = ProjectApiKeyAdminRightsTestData.EXISTING_KEY,
        translations =
          mapOf("en" to "Hello"),
      ),
    ).andIsForbidden
  }

  @Test
  fun `an admin's key is held to the admin's own per-language restriction on the write path`() {
    // The reporting endpoint below says the key is restricted to German; this is the half that enforces it.
    val restrictedKey = key(testData.languageRestrictedAdmin, Scope.TRANSLATIONS_EDIT)

    performPut(
      "$translationsUrl?ak=$restrictedKey",
      SetTranslationsWithKeyDto(
        key = ProjectApiKeyAdminRightsTestData.EXISTING_KEY,
        translations =
          mapOf("en" to "Hello"),
      ),
    ).andIsForbidden

    performPut(
      "$translationsUrl?ak=$restrictedKey",
      SetTranslationsWithKeyDto(
        key = ProjectApiKeyAdminRightsTestData.EXISTING_KEY,
        translations =
          mapOf("de" to "Hallo"),
      ),
    ).andIsOk
  }

  @Test
  fun `current-key permissions report the real language restriction, not the admin's reach`() {
    // /v2/api-keys/current answers for the calling key, so it must describe what that key can actually do. Reporting
    // the admin-bypassed set here would contradict what the write path now enforces.
    performGet("/v2/api-keys/current?ak=${key(testData.languageRestrictedAdmin, Scope.TRANSLATIONS_EDIT)}")
      .andIsOk
      .andAssertThatJson {
        node("permittedLanguageIds").isArray.hasSize(1)
        node("permittedLanguageIds").isArray.contains(testData.german.id)
      }
  }

  @Test
  fun `the project model reports what the key can reach, not the admin elevation behind it`() {
    val memberKey = key(testData.memberAdmin, Scope.TRANSLATIONS_VIEW)

    performGet("/v2/projects/${testData.project.id}?ak=$memberKey")
      .andIsOk
      .andAssertThatJson {
        node("computedPermission.scopes").isArray.doesNotContain("admin")
        node("computedPermission.origin").isEqualTo("DIRECT")
      }
  }

  private val translationsUrl: String
    get() = "/v2/projects/${testData.project.id}/translations"

  private fun key(
    user: UserAccount,
    vararg scopes: Scope,
  ): String = apiKeyService.create(user, scopes.toSet(), testData.projectBuilder.self).key!!
}
