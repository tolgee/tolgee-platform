package io.tolgee.api.v2.controllers.apps

import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.service.apps.AppsTestFixtures
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

/**
 * An app writing on its own behalf must not be recorded as the person who registered it: they did
 * not make the change, and they may not even work here any more.
 */
class AppActivityAttributionTest : AuthorizedControllerTest() {
  @MockitoBean
  @Autowired
  lateinit var appManifestHttpClient: AppManifestHttpClient

  lateinit var testData: AppsTestData
  var installId: Long = 0
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
    performAuthPut("/v2/projects/${testData.project.id}/apps/$installId", null).andIsOk
    installToken = requestInstallToken(json.get("clientId").asText(), json.get("clientSecret").asText())
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `records the install as the actor and nobody as the author`() {
    createKeyAsApp("made-by-the-app")

    asApp(get("/v2/projects/${testData.project.id}/activity")).andIsOk.andAssertThatJson {
      node("_embedded.activities[0].app.installId").isEqualTo(installId)
      node("_embedded.activities[0].app.appId").isEqualTo("test-app")
      node("_embedded.activities[0].app.name").isEqualTo("Test App")
      node("_embedded.activities[0].author").isNull()
    }
  }

  /** Narrowed to a person, the change really is theirs — and the app is still named. */
  @Test
  fun `records both the acted-as user and the install`() {
    logout()
    perform(
      post("/v2/projects/${testData.project.id}/translations")
        .header(HttpHeaders.AUTHORIZATION, "Bearer $installToken")
        .header(ACT_AS_USER_HEADER, testData.user.id.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"key":"made-for-a-user","translations":{"en":"Hello"}}"""),
    ).andIsOk

    asApp(get("/v2/projects/${testData.project.id}/activity")).andIsOk.andAssertThatJson {
      node("_embedded.activities[0].app.installId").isEqualTo(installId)
      node("_embedded.activities[0].author.id").isEqualTo(testData.user.id)
    }
  }

  @Test
  fun `leaves a person's own change attributed to them and to no app`() {
    userAccount = testData.user
    performAuthPost(
      "/v2/projects/${testData.project.id}/translations",
      mapOf("key" to "made-by-a-person", "translations" to mapOf("en" to "Hello")),
    ).andIsOk

    performAuthGet("/v2/projects/${testData.project.id}/activity").andIsOk.andAssertThatJson {
      node("_embedded.activities[0].author.id").isEqualTo(testData.user.id)
      node("_embedded.activities[0].app").isNull()
    }
  }

  private fun createKeyAsApp(key: String) {
    asApp(
      post("/v2/projects/${testData.project.id}/translations")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"key":"$key","translations":{"en":"Hello"}}"""),
    ).andIsOk
  }

  private fun asApp(builder: MockHttpServletRequestBuilder): ResultActions {
    logout()
    return perform(builder.header(HttpHeaders.AUTHORIZATION, "Bearer $installToken"))
  }

  private fun requestInstallToken(
    clientId: String,
    clientSecret: String,
  ): String {
    logout()
    val response =
      perform(
        post("/v2/public/apps/token")
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            objectMapper.writeValueAsString(
              mapOf(
                "grant_type" to "client_credentials",
                "client_id" to clientId,
                "client_secret" to clientSecret,
              ),
            ),
          ),
      ).andIsOk.andReturn().response.contentAsString
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
        "scopes": ["keys.edit", "translations.edit", "activity.view"],
        "modules": {
          "project-dashboard-page": [
            {"key": "home", "title": "Home", "icon": "🏠", "entry": "/"}
          ]
        }
      }
      """.trimIndent()
  }
}
