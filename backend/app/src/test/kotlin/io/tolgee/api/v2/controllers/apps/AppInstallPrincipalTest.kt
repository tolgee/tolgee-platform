package io.tolgee.api.v2.controllers.apps

import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.dtos.request.organization.SetOrganizationRoleDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.service.apps.AppsTestFixtures
import io.tolgee.service.apps.lifecycle.AppLifecycleHttpClient
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import tools.jackson.databind.JsonNode

/**
 * An install acts as an account of its own, so writing a row that references a user works whatever
 * became of the person who registered the app — and that account is not a person: it takes no seat,
 * appears in no listing, cannot be signed in as, and goes when the install goes.
 */
class AppInstallPrincipalTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appInstallService: AppInstallService

  @MockitoBean
  @Autowired
  lateinit var appManifestHttpClient: AppManifestHttpClient

  @MockitoBean
  @Autowired
  lateinit var appLifecycleHttpClient: AppLifecycleHttpClient

  lateinit var testData: AppsTestData
  var installId: Long = 0
  var principalId: Long = 0
  var keyId: Long = 0
  lateinit var installToken: String

  @BeforeEach
  fun setup() {
    testData = AppsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    AppsTestFixtures.mockManifest(appManifestHttpClient, MANIFEST)

    val json =
      objectMapper.readTree(
        performAuthPost(
          "/v2/organizations/${testData.organization.id}/apps/register",
          mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL),
        ).andIsOk.andReturn().response.contentAsString,
      )
    installId = json.get("id").asLong()
    principalId = appInstallService.resolveForAppAuth(installId)!!.principal.id
    performAuthPut("/v2/projects/${testData.project.id}/apps/$installId", null).andIsOk
    installToken = requestInstallToken(json)

    keyId =
      objectMapper
        .readTree(
          performAuthPost(
            "/v2/projects/${testData.project.id}/translations",
            mapOf("key" to "commented-key", "translations" to mapOf("en" to "Hello")),
          ).andIsOk.andReturn().response.contentAsString,
        ).get("keyId")
        .asLong()
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `writes a comment as itself rather than as the person who registered it`() {
    principalId.assert.isNotEqualTo(testData.user.id)

    commentAsApp().andIsCreated.andAssertThatJson {
      node("comment.author.id").isEqualTo(principalId)
    }
  }

  @Test
  fun `writes a comment while its author is disabled`() {
    userAccountService.disable(testData.user.id)

    commentAsApp().andIsCreated
  }

  @Test
  fun `writes a comment while its author is deleted`() {
    // Somebody else has to own the organization first, or deleting its only owner takes it with them.
    organizationRoleService.setMemberRole(
      testData.organization.id,
      testData.member.id,
      SetOrganizationRoleDto(OrganizationRoleType.OWNER),
    )
    userAccountService.delete(testData.user.id)

    commentAsApp().andIsCreated
  }

  /** The principal is what the install acts as, so it must be obvious in any UI that shows it. */
  @Test
  fun `names the principal after the app`() {
    userAccountService
      .findActive(principalId)!!
      .name.assert
      .isEqualTo("Test App [app]")
  }

  @Test
  fun `takes no seat`() {
    val seats = userAccountService.countAllEnabled()

    AppsTestFixtures.mockManifest(appManifestHttpClient, SECOND_MANIFEST)
    performAuthPost(
      "/v2/organizations/${testData.organization.id}/apps/register",
      mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL),
    ).andIsOk

    userAccountService.countAllEnabled().assert.isEqualTo(seats)
  }

  @Test
  fun `is listed neither among the organization's members nor among the server's users`() {
    val members: List<Long> =
      objectMapper
        .readTree(
          performAuthGet("/v2/organizations/${testData.organization.id}/users?size=1000")
            .andIsOk
            .andReturn()
            .response.contentAsString,
        ).path("_embedded")
        .path("usersInOrganization")
        .values()
        .map { it.path("id").asLong() }

    members.assert.contains(testData.user.id)
    members.assert.doesNotContain(principalId)

    userAccountService
      .findAllWithDisabledPaged(Pageable.ofSize(1000), null)
      .content
      .map { it.id }
      .assert
      .doesNotContain(principalId)
  }

  @Test
  fun `cannot be signed in as`() {
    val username = userAccountService.findActive(principalId)!!.username

    userAccountService.findActive(username).assert.isNull()
    userAccountService.findActiveOrDisabled(username).assert.isNull()
  }

  @Test
  fun `is retired when the install is removed`() {
    performAuthDelete("/v2/organizations/${testData.organization.id}/apps/$installId").andIsOk

    userAccountService.findActive(principalId).assert.isNull()
  }

  /**
   * Acting as a person is the one path where a person's state still gates the request — the
   * install's own principal must not paper over it.
   */
  @Test
  fun `refuses to act as a disabled user`() {
    userAccountService.disable(testData.member.id)

    logout()
    perform(
      commentRequest()
        .header(HttpHeaders.AUTHORIZATION, "Bearer $installToken")
        .header(ACT_AS_USER_HEADER, testData.member.id.toString()),
    ).andIsForbidden.andHasErrorMessage(Message.APP_ACTING_AS_USER_NOT_PROJECT_MEMBER)
  }

  @Test
  fun `keeps attributing its own change to the install and to nobody`() {
    userAccountService.disable(testData.user.id)
    commentAsApp().andIsCreated

    // [0] is the key the owner created in setup; the app's comment follows it.
    asApp(get("/v2/projects/${testData.project.id}/activity")).andIsOk.andAssertThatJson {
      node("_embedded.activities[1].type").isEqualTo("TRANSLATION_COMMENT_ADD")
      node("_embedded.activities[1].app.installId").isEqualTo(installId)
      node("_embedded.activities[1].author").isNull()
    }
  }

  private fun commentAsApp(): ResultActions = asApp(commentRequest())

  private fun commentRequest(): MockHttpServletRequestBuilder {
    return post("/v2/projects/${testData.project.id}/translations/create-comment")
      .contentType(MediaType.APPLICATION_JSON)
      .content(
        objectMapper.writeValueAsString(
          mapOf(
            "keyId" to keyId,
            "languageId" to testData.englishLanguage.id,
            "text" to "written by the app",
          ),
        ),
      )
  }

  private fun asApp(builder: MockHttpServletRequestBuilder): ResultActions {
    logout()
    return perform(builder.header(HttpHeaders.AUTHORIZATION, "Bearer $installToken"))
  }

  private fun requestInstallToken(registration: JsonNode): String {
    logout()
    val response =
      perform(
        post("/v2/public/apps/token")
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            objectMapper.writeValueAsString(
              mapOf(
                "grant_type" to "client_credentials",
                "client_id" to registration.at("/app/clientId").asText(),
                "client_secret" to registration.at("/app/clientSecret").asText(),
                "install_id" to registration.get("id").asLong(),
              ),
            ),
          ),
      ).andIsOk.andReturn().response.contentAsString
    userAccount = testData.user
    return objectMapper.readTree(response).get("access_token").asText()
  }

  companion object {
    private const val ACT_AS_USER_HEADER = "X-Tolgee-Act-As-User-Id"

    private val MANIFEST: String =
      """
      {
        "id": "test-app",
        "name": "Test App",
        "version": "0.1.0",
        "baseUrl": "https://app.example.com",
        "scopes": ["translations.edit", "translation-comments.add", "activity.view"],
        "modules": {
          "project-dashboard-page": [
            {"key": "home", "title": "Home", "icon": "🏠", "entry": "/"}
          ]
        }
      }
      """.trimIndent()

    private val SECOND_MANIFEST: String =
      """
      {
        "id": "second-app",
        "name": "Second App",
        "version": "0.1.0",
        "baseUrl": "https://second.example.com",
        "scopes": ["translations.edit"],
        "modules": {
          "project-dashboard-page": [
            {"key": "home", "title": "Home", "icon": "🏠", "entry": "/"}
          ]
        }
      }
      """.trimIndent()
  }
}
