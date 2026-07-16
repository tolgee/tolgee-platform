package io.tolgee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.ProjectPublishingTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class CommunityPermissionTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: ProjectPublishingTestData

  @BeforeEach
  fun setup() {
    testData = ProjectPublishingTestData()
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.project }
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `authenticated non-member gets the community permission on a public project`() {
    setPublic(true)
    userAccount = testData.communityUser
    performAuthGet(projectUrl()).andIsOk.andAssertThatJson {
      node("computedPermission.origin").isEqualTo("COMMUNITY")
      node("computedPermission.scopes").isArray.contains(
        "translations.view",
        "screenshots.view",
        "activity.view",
        "translations.suggest",
        "translation-comments.add",
      )
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `community permission never grants write or restricted scopes`() {
    setPublic(true)
    userAccount = testData.communityUser
    performAuthGet(projectUrl()).andIsOk.andAssertThatJson {
      node("computedPermission.scopes").isArray.isNotEmpty
      node("computedPermission.scopes")
        .isArray
        .doesNotContain(
          "translations.edit",
          "keys.create",
          "keys.edit",
          "keys.delete",
          "members.view",
          "organization-quotas.view",
        )
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `non-member cannot access a private project`() {
    userAccount = testData.communityUser
    performAuthGet(projectUrl()).andIsNotFound
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `un-publishing revokes community access`() {
    setPublic(true)
    userAccount = testData.communityUser
    performAuthGet(projectUrl()).andIsOk

    setPublic(false)
    performAuthGet(projectUrl()).andIsNotFound
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `member count is hidden from community users`() {
    setPublic(true)
    userAccount = testData.communityUser
    performProjectAuthGet("stats").andIsOk.andAssertThatJson {
      node("membersCount").isEqualTo(0)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `member count is hidden from members lacking members-view`() {
    userAccount = testData.viewer
    performProjectAuthGet("stats").andIsOk.andAssertThatJson {
      node("membersCount").isEqualTo(0)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `member count and credits stay visible to members with members-view`() {
    setPublic(true)
    userAccount = testData.manager
    performProjectAuthGet("stats").andIsOk.andAssertThatJson {
      node("membersCount").isEqualTo(6)
    }
    performProjectAuthGet("machine-translation-credit-balance").andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `community user cannot read organization MT credit balance`() {
    setPublic(true)
    userAccount = testData.communityUser
    performProjectAuthGet("machine-translation-credit-balance").andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `community user cannot store import settings`() {
    setPublic(true)
    userAccount = testData.communityUser
    performProjectAuthPut("import-settings", mapOf<String, Any>()).andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a member with translations-edit can store import settings`() {
    userAccount = testData.translationsEditor
    storeImportSettings().andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a member with only keys-create cannot store import settings`() {
    userAccount = testData.keysCreator
    storeImportSettings().andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a member with only translations-view cannot store import settings`() {
    userAccount = testData.granularUser
    storeImportSettings().andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `anonymous user cannot open a public project`() {
    setPublic(true)
    performGet(projectUrl()).andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `anonymous user cannot list translations of a public project`() {
    setPublic(true)
    performGet("${projectUrl()}/translations").andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `community user cannot change translations`() {
    setPublic(true)
    userAccount = testData.communityUser
    performProjectAuthPut(
      "translations",
      mapOf(
        "key" to "trash-me",
        "translations" to mapOf("en" to "hacked"),
      ),
    ).andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `community user cannot create keys`() {
    setPublic(true)
    userAccount = testData.communityUser
    performProjectAuthPost("keys", mapOf("name" to "community-key")).andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `community user cannot list members`() {
    setPublic(true)
    userAccount = testData.communityUser
    performProjectAuthGet("users").andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `granular member without the credits scope cannot read MT credit balance`() {
    userAccount = testData.granularUser
    performProjectAuthGet("machine-translation-credit-balance").andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a narrow direct permission is floored with the community scopes on a public project`() {
    setPublic(true)
    userAccount = testData.granularUser
    performAuthGet(projectUrl()).andIsOk.andAssertThatJson {
      node("computedPermission.origin").isEqualTo("DIRECT")
      node("computedPermission.scopes").isArray.contains(
        "translations.view",
        "screenshots.view",
        "activity.view",
        "translations.suggest",
        "translation-comments.add",
      )
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a VIEW member is floored with community suggest and comment on a public project`() {
    setPublic(true)
    userAccount = testData.viewer
    performAuthGet(projectUrl()).andIsOk.andAssertThatJson {
      node("computedPermission.origin").isEqualTo("DIRECT")
      node("computedPermission.scopes").isArray.contains(
        "translations.suggest",
        "translation-comments.add",
      )
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `community user may mint an API key within community scopes (intentional for v1)`() {
    setPublic(true)
    userAccount = testData.communityUser
    performAuthPost(
      "/v2/api-keys",
      mapOf(
        "projectId" to testData.project.id,
        "scopes" to setOf("translations.suggest"),
      ),
    ).andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `community user cannot mint an API key with scopes outside the community set`() {
    setPublic(true)
    userAccount = testData.communityUser
    performAuthPost(
      "/v2/api-keys",
      mapOf(
        "projectId" to testData.project.id,
        "scopes" to setOf("translations.edit"),
      ),
    ).andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `key-deleter email is hidden from community in the trash listing`() {
    setPublic(true)
    val key = keyService.get(testData.project.id, "trash-me", null)
    executeInNewTransaction {
      keyService.softDeleteMultiple(listOf(key.id), deletedBy = testData.manager)
    }

    userAccount = testData.communityUser
    performProjectAuthGet("keys/trash").andIsOk.andAssertThatJson {
      node("_embedded.keys[0].deletedBy.username").isEqualTo("")
      node("_embedded.keys[0].deletedBy.name").isEqualTo("Project Manager")
    }

    userAccount = testData.manager
    performProjectAuthGet("keys/trash").andIsOk.andAssertThatJson {
      node("_embedded.keys[0].deletedBy.username").isEqualTo("project_manager")
    }
  }

  private fun storeImportSettings() =
    performProjectAuthPut(
      "import-settings",
      mapOf(
        "overrideKeyDescriptions" to true,
        "createNewKeys" to false,
      ),
    )

  private fun setPublic(public: Boolean) {
    projectService.setPublic(testData.project.id, public)
  }

  private fun projectUrl() = "/v2/projects/${testData.project.id}"
}
