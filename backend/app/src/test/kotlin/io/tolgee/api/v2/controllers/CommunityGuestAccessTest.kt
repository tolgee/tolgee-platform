package io.tolgee.api.v2.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.development.testDataBuilder.data.CommunityGuestTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andGetContentAsString
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.fixtures.node
import io.tolgee.model.Project
import io.tolgee.security.authentication.TolgeeAuthentication
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.ResultActions

@SpringBootTest
@AutoConfigureMockMvc
class CommunityGuestAccessTest : AuthorizedControllerTest() {
  private lateinit var testData: CommunityGuestTestData

  @BeforeEach
  fun setup() {
    testData = CommunityGuestTestData()
    testDataService.saveTestData(testData.root)
    executeInNewTransaction { testData.replayActivity(entityManager) }
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `a guest opens a public project with the read-only guest floor`() {
    performGet(url(testData.publicProject)).andIsOk.andAssertThatJson {
      node("id").isEqualTo(testData.publicProject.id)
      node("directPermission").isEqualTo(null)
      node("organizationRole").isEqualTo(null)
      node("computedPermission.type").isEqualTo("VIEW")
      node("computedPermission.origin").isEqualTo("GUEST")
      node("computedPermission.scopes").isArray.containsExactlyInAnyOrder(
        "keys.view",
        "translations.view",
        "screenshots.view",
        "activity.view",
      )
    }
  }

  @Test
  fun `a guest reads what the translations view and the community pages need`() {
    val project = url(testData.publicProject)
    performGet("$project/translations").andIsOk.andAssertThatJson {
      node("_embedded.keys").isArray.hasSize(3)
      node("selectedLanguages").isArray.isNotEmpty
      node("_embedded.keys[0].translations.en.text").isEqualTo("Hello")
    }
    performGet("$project/translations/en,cs").andIsOk.andAssertThatJson {
      node("en").isObject.containsValue("Hello")
    }
    performGet("$project/languages").andIsOk
    performGet("$project/stats").andIsOk.andAssertThatJson {
      node("membersCount").isEqualTo(0)
    }
    performGet("$project/machine-translation-service-settings").andIsOk
    performGet("$project/keys/${testData.publicKeys[0].self.id}/screenshots").andIsOk
    performGet("$project/community/contributors").andIsOk.andAssertThatJson {
      node("_embedded.contributors").isArray.isNotEmpty
      node("_embedded.contributors[0].contributions").isNumber
    }
    performGet("$project/community/activity?granularity=DAY").andIsOk.andAssertThatJson {
      node("buckets").isArray.isNotEmpty
    }
    performGet("/v2/users/${testData.alice.id}/community-profile").andIsOk.andAssertThatJson {
      node("id").isEqualTo(testData.alice.id)
    }
    performGet("/v2/users/${testData.alice.id}/community-activity").andIsOk.andAssertThatJson {
      node("_embedded.activities").isArray.isNotEmpty
    }
  }

  @Test
  fun `a guest cannot write, 401 where a scope gates the handler and 403 where the handler checks itself`() {
    val project = url(testData.publicProject)
    val key = testData.publicKeys[0].self
    performPost(
      "$project/languages/${testData.czech.id}/key/${key.id}/suggestion",
      mapOf("translation" to "guest suggestion"),
    ).andIsUnauthorized
    performPut("$project/leave", null).andIsUnauthorized
    performDelete(project, null).andIsUnauthorized

    performPost(
      "$project/translations/create-comment",
      mapOf("keyId" to key.id, "languageId" to testData.czech.id, "text" to "guest comment"),
    ).andIsForbidden
    performPut(
      "$project/translations",
      mapOf("key" to key.name, "translations" to mapOf("cs" to "guest edit")),
    ).andIsForbidden
    performPut(
      "$project/keys/${key.id}/complex-update",
      mapOf("name" to "renamed-by-guest"),
    ).andIsForbidden
  }

  @Test
  fun `the trash is closed to a guest and stays open to a signed-in non-member`() {
    val project = url(testData.publicProject)
    val trash = "$project/keys/trash"
    userAccount = testData.user
    performAuthDelete("$project/keys/${testData.publicKeys[2].self.id}").andIsOk

    performGet(trash).andIsUnauthorized
    performGet("$trash/select-all").andIsUnauthorized
    performGet("$trash/deleters").andIsUnauthorized
    performGet("$project/translations?trashed=true").andIsOk.andAssertThatJson {
      node("_embedded.keys").isArray.hasSize(2)
    }

    userAccount = testData.bob
    performAuthGet(trash).andIsOk.andAssertThatJson {
      node("_embedded.keys").isArray.hasSize(1)
    }
  }

  @Test
  fun `the activity log shows translation work only to whoever sees it just because the project is public`() {
    val project = url(testData.publicProject)
    userAccount = testData.user
    performAuthPost("$project/keys/create", mapOf("name" to "configured-by-the-owner")).andIsCreated
    val revisionId =
      executeInNewTransaction {
        entityManager
          .createNativeQuery("select id from activity_revision where project_id = :id and type = 'CREATE_KEY'")
          .setParameter("id", testData.publicProject.id)
          .singleResult
      }
    assertThat(activityTypes(performAuthGet("$project/activity?size=100"))).contains("CREATE_KEY")
    performAuthGet("$project/activity/revisions/$revisionId").andIsOk
    performAuthGet("$project/activity/revisions/$revisionId/modified-entities").andIsOk
    performAuthGet("$project/activity/revisions/999999999/modified-entities").andIsOk

    fun assertTranslationWorkOnly(get: (String) -> ResultActions) {
      assertThat(activityTypes(get("$project/activity?size=100")))
        .contains("CREATE_SUGGESTION")
        .doesNotContain("CREATE_KEY")
      get("$project/activity/revisions/$revisionId").andIsNotFound
      get("$project/activity/revisions/$revisionId/modified-entities").andIsNotFound
    }
    assertTranslationWorkOnly { performGet(it) }
    userAccount = testData.bob
    assertTranslationWorkOnly { performAuthGet(it) }
    userAccount = testData.memberWithoutActivityScope
    assertTranslationWorkOnly { performAuthGet(it) }
  }

  @Test
  fun `a server admin sees the whole activity log signed in, and translation work only through a project API key`() {
    val activity = "${url(testData.publicProject)}/activity?size=100"
    userAccount = testData.user
    performAuthPost("${url(testData.publicProject)}/keys/create", mapOf("name" to "configured-by-the-owner"))
      .andIsCreated
    val projectApiKey =
      HttpHeaders().apply { add("X-API-Key", "tgpak_" + testData.serverAdminApiKey.encodedKey) }

    userAccount = testData.serverAdmin
    assertThat(activityTypes(performAuthGet(activity))).contains("CREATE_KEY")
    assertThat(activityTypes(performGet(activity, projectApiKey)))
      .contains("CREATE_SUGGESTION")
      .doesNotContain("CREATE_KEY")
  }

  @Test
  fun `a guest is told to sign in to export a public project, also on a conditional request`() {
    val project = url(testData.publicProject)
    performGet("$project/export?zip=false&languages=en&format=JSON").andIsUnauthorized
    performPost("$project/export", mapOf("languages" to listOf("en"), "format" to "JSON", "zip" to false))
      .andIsUnauthorized
    val notModifiedSince = HttpHeaders().apply { set("If-Modified-Since", "Fri, 01 Jan 2100 00:00:00 GMT") }
    performGet("$project/export?zip=false&languages=en&format=JSON", notModifiedSince).andIsUnauthorized
  }

  @Test
  fun `a guest is told to sign in on everything keyed on an account or outside a project`() {
    performGet("/v2/projects").andIsUnauthorized
    performGet("/v2/projects/translations").andIsUnauthorized
    performGet("/v2/slug/validate-organization/some-slug").andIsUnauthorized
    performGet("/v2/user-tasks").andIsUnauthorized
    performGet("/v2/projects/with-stats").andIsUnauthorized
    performGet("/v2/user").andIsUnauthorized
    performGet("/v2/user-preferences").andIsUnauthorized
    performGet("/v2/organizations").andIsUnauthorized
    performGet("${url(testData.publicProject)}/import-settings").andIsUnauthorized
  }

  @Test
  fun `public endpoints answer a guest as they answer anyone`() {
    performGet("/v2/public/initial-data").andIsOk
  }

  @Test
  fun `a guest is told to sign in for a private or missing project, not that it does not exist`() {
    val privateProject = url(testData.privateProject)
    performGet(privateProject).andIsUnauthorized
    performGet("$privateProject/translations").andIsUnauthorized
    performGet("$privateProject/community/contributors").andIsUnauthorized

    performGet("/v2/projects/999999999").andIsUnauthorized
    performGet("/v2/projects/999999999/translations").andIsUnauthorized

    userAccount = testData.bob
    performAuthGet(privateProject).andIsNotFound
    performAuthGet("/v2/projects/999999999").andIsBadRequest
  }

  @Test
  fun `a profile hidden from members is hidden from guests the same way`() {
    performGet("/v2/users/${testData.carol.id}/community-profile").andIsNotFound
    performGet("/v2/users/${testData.carol.id}/community-activity").andIsNotFound
  }

  @Test
  fun `a guest sees exactly what a signed-in non-member sees of public projects the listing hides`() {
    listOf(
      testData.softDeletedOrgPublicProject,
      testData.softDeletedBaseProject,
      testData.noBaseLanguageProject,
    ).forEach { project ->
      val guestStatus = performGet(url(project)).andReturn().response.status
      userAccount = testData.bob
      val memberStatus = performAuthGet(url(project)).andReturn().response.status
      guestStatus.assert.describedAs(project.name).isEqualTo(memberStatus)
    }
  }

  @Test
  fun `a soft-deleted project id is a sign-in prompt for a guest and a bad request for a member`() {
    performGet(url(testData.deletedPublicProject)).andIsUnauthorized
    userAccount = testData.bob
    performAuthGet(url(testData.deletedPublicProject)).andIsBadRequest
  }

  @Test
  fun `a guest's reads leave no row under the guest id`() {
    performGet(url(testData.publicProject)).andIsOk
    performGet("${url(testData.publicProject)}/translations").andIsOk

    rowsAttributedToGuest("activity_revision", "author_id").assert.isZero()
    rowsAttributedToGuest("project_contributor", "user_id").assert.isZero()
  }

  private fun activityTypes(response: ResultActions): List<String> =
    jacksonObjectMapper()
      .readTree(response.andIsOk.andGetContentAsString)
      .path("_embedded")
      .path("activities")
      .map { it.path("type").asText() }

  private fun url(project: Project) = "/v2/projects/${project.id}"

  private fun rowsAttributedToGuest(
    table: String,
    column: String,
  ): Long =
    executeInNewTransaction {
      (
        entityManager
          .createNativeQuery("select count(*) from $table where $column = :id")
          .setParameter("id", TolgeeAuthentication.GUEST_USER_ID)
          .singleResult as Number
      ).toLong()
    }
}
