package io.tolgee.api.v2.controllers.translations

import io.tolgee.API_KEY_HEADER_NAME
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.TranslationCommentsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.model.ApiKey
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.model.translation.Translation
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import java.util.Date

@SpringBootTest
@AutoConfigureMockMvc
class TranslationCommentControllerGetAllTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TranslationCommentsTestData
  lateinit var userWithNoAccess: UserAccount
  lateinit var otherProject: Project
  lateinit var otherProjectTranslation: Translation
  lateinit var otherProjectApiKey: ApiKey
  lateinit var otherProjectOwner: UserAccount

  @BeforeEach
  fun setup(testInfo: TestInfo) {
    testData = TranslationCommentsTestData()

    // Add user without any project access
    testData.root.apply {
      addUserAccount {
        username = "user_without_access"
        userWithNoAccess = this
      }

      // Create another project for cross-project tests
      val otherUserBuilder =
        addUserAccount {
          username = "other_project_owner"
          otherProjectOwner = this
        }

      addProject {
        name = "Other project"
        organizationOwner = otherUserBuilder.defaultOrganizationBuilder.self
        otherProject = this
      }.build {
        addPermission {
          user = otherUserBuilder.self
          type = ProjectPermissionType.MANAGE
        }

        val lang =
          addLanguage {
            name = "English"
            tag = "en"
            originalName = "English"
          }

        addKey {
          name = "other-key"
        }.build {
          addTranslation {
            language = lang.self
            text = "Other translation"
            otherProjectTranslation = this
          }
        }

        addApiKey {
          key = "other_project_api_key"
          scopesEnum = Scope.values().toMutableSet()
          userAccount = otherUserBuilder.self
          otherProjectApiKey = this
        }
      }
    }

    // Give the other project owner VIEW permission on the main test project
    // so we can test PAK mismatch (403) instead of user permission denied (404)
    testData.projectBuilder.apply {
      addPermission {
        user = otherProjectOwner
        type = ProjectPermissionType.VIEW
      }
    }

    testDataService.saveTestData(testData.root)

    // Only set projectSupplier for tests that have @ProjectJWTAuthTestMethod annotation
    val method = testInfo.testMethod.orElse(null)
    if (method?.getAnnotation(ProjectJWTAuthTestMethod::class.java) != null) {
      this.projectSupplier = { testData.project }
      userAccount = testData.user
    }
  }

  // ==================== Success Tests (200) ====================

  @ProjectJWTAuthTestMethod
  @Test
  fun `returns 200 with paginated comments`() {
    performProjectAuthGet("translations/${testData.translation.id}/comments")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(2)
        node("_embedded.translationComments").isArray.hasSize(2)
      }
  }

  // ==================== Authentication Tests (401/403) ====================

  @Test
  fun `returns 403 when no token provided`() {
    // Project-level endpoints without any authentication return 403 Forbidden
    performGet("/v2/projects/${testData.project.id}/translations/${testData.translation.id}/comments")
      .andIsForbidden
  }

  @Test
  fun `returns 401 when invalid JWT token`() {
    performGet(
      "/v2/projects/${testData.project.id}/translations/${testData.translation.id}/comments",
      HttpHeaders().apply {
        add("Authorization", "Bearer invalid_jwt_token_here")
      },
    ).andIsUnauthorized
      .andAssertThatJson {
        node("code").isEqualTo("invalid_jwt_token")
      }
  }

  @Test
  fun `returns 401 when expired JWT token`() {
    // Generate a token in the past that's now expired
    val baseline = Date()
    currentDateProvider.forcedDate = Date(baseline.time - tolgeeProperties.authentication.jwtExpiration - 10_000)
    val expiredToken = jwtService.emitToken(testData.user.id)
    currentDateProvider.forcedDate = baseline

    performGet(
      "/v2/projects/${testData.project.id}/translations/${testData.translation.id}/comments",
      HttpHeaders().apply {
        add("Authorization", "Bearer $expiredToken")
      },
    ).andIsUnauthorized
      .andAssertThatJson {
        node("code").isEqualTo("expired_jwt_token")
      }
  }

  @Test
  fun `returns 401 when invalid PAK format`() {
    performGet(
      "/v2/projects/${testData.project.id}/translations/${testData.translation.id}/comments",
      HttpHeaders().apply {
        add(API_KEY_HEADER_NAME, "tgpak_---invalid_base64")
      },
    ).andIsUnauthorized
      .andAssertThatJson {
        node("code").isEqualTo("invalid_project_api_key")
      }
  }

  @Test
  fun `returns 401 when expired PAK`() {
    // Create an expired API key
    var expiredApiKey: ApiKey? = null
    testData.projectBuilder.apply {
      addApiKey {
        key = "expired_pak_test"
        scopesEnum = Scope.values().toMutableSet()
        userAccount = testData.user
        expiresAt = Date(System.currentTimeMillis() - 10_000) // Expired 10 seconds ago
        expiredApiKey = this
      }
    }
    testDataService.saveTestData(testData.root)

    performGet(
      "/v2/projects/${testData.project.id}/translations/${testData.translation.id}/comments",
      HttpHeaders().apply {
        add(API_KEY_HEADER_NAME, "tgpak_${expiredApiKey!!.encodedKey}")
      },
    ).andIsUnauthorized
      .andAssertThatJson {
        node("code").isEqualTo("project_api_key_expired")
      }
  }

  // ==================== Project Authorization Tests (400/404/403) ====================

  @ProjectJWTAuthTestMethod
  @Test
  fun `returns 400 when project does not exist`() {
    performAuthGet("/v2/projects/999999999/translations/${testData.translation.id}/comments")
      .andIsBadRequest
      .andAssertThatJson {
        node("code").isEqualTo("project_not_selected")
      }
  }

  @Test
  fun `returns 404 when user has no project permission`() {
    loginAsUser(userWithNoAccess.username)
    performAuthGet("/v2/projects/${testData.project.id}/translations/${testData.translation.id}/comments")
      .andIsNotFound
      .andAssertThatJson {
        node("code").isEqualTo("project_not_found")
      }
  }

  @Test
  fun `returns 403 when PAK for different project`() {
    performGet(
      "/v2/projects/${testData.project.id}/translations/${testData.translation.id}/comments",
      HttpHeaders().apply {
        add(API_KEY_HEADER_NAME, "tgpak_${otherProjectApiKey.encodedKey}")
      },
    ).andIsForbidden
      .andAssertThatJson {
        node("code").isEqualTo("pak_created_for_different_project")
      }
  }

  // ==================== Business Logic Tests (404) ====================

  @ProjectJWTAuthTestMethod
  @Test
  fun `returns 404 when translation does not exist`() {
    performProjectAuthGet("translations/999999999/comments")
      .andIsNotFound
      .andAssertThatJson {
        node("code").isEqualTo("translation_not_found")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `returns 404 when translation belongs to different project`() {
    performProjectAuthGet("translations/${otherProjectTranslation.id}/comments")
      .andIsNotFound
      .andAssertThatJson {
        node("code").isEqualTo("translation_not_found")
      }
  }

}
