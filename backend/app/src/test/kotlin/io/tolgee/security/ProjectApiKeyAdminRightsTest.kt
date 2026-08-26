package io.tolgee.security

import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
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
  private lateinit var testData: BaseTestData
  private lateinit var outsideAdmin: UserAccount
  private lateinit var outsideSupporter: UserAccount
  private lateinit var memberAdmin: UserAccount

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    // A server admin and a supporter who are NOT members of the project.
    outsideAdmin =
      testData.root
        .addUserAccount {
          username = "pak_outside_admin"
          role = UserAccount.Role.ADMIN
        }.self
    outsideSupporter =
      testData.root
        .addUserAccount {
          username = "pak_outside_supporter"
          role = UserAccount.Role.SUPPORTER
        }.self
    // A server admin who IS a member, with only VIEW — to prove the key follows the real membership, not the role.
    memberAdmin =
      testData.root
        .addUserAccount {
          username = "pak_member_admin"
          role = UserAccount.Role.ADMIN
        }.self
    testData.projectBuilder.addPermission {
      user = memberAdmin
      type = ProjectPermissionType.VIEW
    }
    // An existing key, so the write test exercises the permission check rather than a missing-key 404.
    testData.projectBuilder
      .addKey { name = EXISTING_KEY }
      .build {
        addTranslation {
          language = testData.projectBuilder.self.baseLanguage!!
          text = "value"
        }
      }
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `an admin's key cannot read a project the admin never joined`() {
    // Non-member access is masked as 404, same as any stranger.
    performGet("$translationsUrl?ak=${key(outsideAdmin, Scope.TRANSLATIONS_VIEW)}").andIsNotFound
  }

  @Test
  fun `a supporter's key cannot read a project the supporter never joined`() {
    performGet("$translationsUrl?ak=${key(outsideSupporter, Scope.TRANSLATIONS_VIEW)}").andIsNotFound
  }

  @Test
  fun `an admin's key works within the admin's real membership`() {
    performGet("$translationsUrl?ak=${key(memberAdmin, Scope.TRANSLATIONS_VIEW)}").andIsOk
  }

  @Test
  fun `an admin's key cannot exceed the admin's real membership`() {
    // memberAdmin is a VIEW member, so the key cannot write even though the key itself carries TRANSLATIONS_EDIT and
    // the user is a server admin.
    performPut(
      "$translationsUrl?ak=${key(memberAdmin, Scope.TRANSLATIONS_EDIT)}",
      SetTranslationsWithKeyDto(key = EXISTING_KEY, translations = mapOf("en" to "Hello")),
    ).andIsForbidden
  }

  private val translationsUrl: String
    get() = "/v2/projects/${testData.project.id}/translations"

  private fun key(
    user: UserAccount,
    vararg scopes: Scope,
  ): String = apiKeyService.create(user, scopes.toSet(), testData.projectBuilder.self).key!!

  companion object {
    private const val EXISTING_KEY = "pak-admin-rights-key"
  }
}
