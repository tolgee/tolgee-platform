package io.tolgee.api.v2.controllers.apps

import io.tolgee.activity.data.ActivityType
import io.tolgee.component.reporting.OnBusinessEventToCaptureEvent
import io.tolgee.component.reporting.PostHogBusinessEventReporter
import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.service.apps.AppsTestFixtures
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

/**
 * App audit logging: every audit-worthy app operation records an [ActivityRevision] with the acting
 * app on it, and a scope re-consent records the granted-scope change old→new.
 */
class AppActivityTest : AuthorizedControllerTest() {
  @MockitoBean
  @Autowired
  lateinit var appManifestHttpClient: AppManifestHttpClient

  @MockitoBean
  @Autowired
  lateinit var appLifecycleHttpClient: io.tolgee.service.apps.lifecycle.AppLifecycleHttpClient

  @MockitoSpyBean
  @Autowired
  lateinit var postHogReporter: PostHogBusinessEventReporter

  lateinit var testData: AppsTestData
  var installId: Long = 0
  lateinit var appClientId: String
  lateinit var appClientSecret: String

  @BeforeEach
  fun setup() {
    testData = AppsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    mockManifest(manifest(scopes = """"translations.view", "keys.edit""""))

    val json =
      objectMapper.readTree(
        performAuthPost(ownedUrl(), mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL))
          .andIsOk
          .andReturn()
          .response.contentAsString,
      )
    installId = json.get("installId").asLong()
    appClientId = json.get("clientId").asText()
    appClientSecret = json.get("clientSecret").asText()
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `register stores an org-scoped APP_REGISTER revision`() {
    val revision = latestRevision(ActivityType.APP_REGISTER)
    revision.assert.isNotNull
    revision!!.organizationId.assert.isEqualTo(testData.organization.id)
    revision.authorId.assert.isEqualTo(testData.user.id)
  }

  @Test
  fun `install stores an APP_INSTALL revision`() {
    mockManifest(manifest(id = "install-app", name = "Install App", scopes = """"translations.view""""))
    performAuthPost(ownedUrl(), mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL, "install" to false)).andIsOk

    performAuthPost(appsUrl(), mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL)).andIsOk

    latestRevision(ActivityType.APP_INSTALL).assert.isNotNull
  }

  @Test
  fun `enabling an app for a project stores an APP_ENABLE_FOR_PROJECT revision`() {
    performAuthPut("/v2/projects/${testData.project.id}/apps/$installId", null).andIsOk

    val revision = latestRevision(ActivityType.APP_ENABLE_FOR_PROJECT)
    revision.assert.isNotNull
    revision!!.projectId.assert.isEqualTo(testData.project.id)
  }

  @Test
  fun `an owner refresh that widens scopes records the granted-scope change old to new`() {
    mockManifest(manifest(scopes = """"translations.view", "keys.edit", "keys.create""""))

    performAuthPost("${appsUrl()}/$installId/refresh", null).andIsOk

    val revision = latestRevision(ActivityType.APP_UPDATE)
    revision.assert.isNotNull
    revision!!.organizationId.assert.isEqualTo(testData.organization.id)

    val modification = latestAppInstallGrantedScopesModification(ActivityType.APP_UPDATE)
    modification.assert.isNotNull
    @Suppress("UNCHECKED_CAST")
    (modification!!.old as List<String>).assert.containsExactlyInAnyOrder("translations.view", "keys.edit")
    @Suppress("UNCHECKED_CAST")
    (modification.new as List<String>)
      .assert
      .containsExactlyInAnyOrder("translations.view", "keys.edit", "keys.create")
  }

  @Test
  fun `an app-self refresh stores an org-scoped revision without an organization holder`() {
    mockManifest(manifest(scopes = """"translations.view""""))

    asAppToken(post("/v2/apps/self/installations/$installId/refresh")).andIsOk

    val revision = latestRevision(ActivityType.APP_UPDATE)
    revision.assert.isNotNull
    revision!!.organizationId.assert.isEqualTo(testData.organization.id)
    revision.authorId.assert.isNotEqualTo(testData.user.id)
  }

  @Test
  fun `the business-event bridge carries the app id and name`() {
    mockManifest(manifest(name = "Renamed App", scopes = """"translations.view", "keys.edit""""))
    performAuthPost("${appsUrl()}/$installId/refresh", null).andIsOk

    val captor = argumentCaptor<OnBusinessEventToCaptureEvent>()
    verify(postHogReporter, atLeastOnce()).capture(captor.capture())

    val event =
      captor.allValues.firstOrNull { it.eventName == ActivityType.APP_UPDATE.name }
    event.assert.isNotNull
    event!!.data!!["appName"].assert.isEqualTo("Renamed App")
    (event.data!!["appId"] as String).toLong().assert.isEqualTo(appEntityId("test-app"))
  }

  private fun latestRevision(type: ActivityType): ActivityRevision? =
    executeInNewTransaction {
      entityManager
        .createQuery(
          "from ActivityRevision ar where ar.type = :type order by ar.id desc",
          ActivityRevision::class.java,
        ).setParameter("type", type)
        .setMaxResults(1)
        .resultList
        .firstOrNull()
    }

  private fun latestAppInstallGrantedScopesModification(type: ActivityType) =
    executeInNewTransaction {
      entityManager
        .createQuery(
          """
          from ActivityModifiedEntity ame
          join ame.activityRevision ar
          where ar.type = :type and ame.entityClass = 'AppInstall'
          order by ar.id desc
          """,
          ActivityModifiedEntity::class.java,
        ).setParameter("type", type)
        .setMaxResults(1)
        .resultList
        .firstOrNull()
        ?.modifications
        ?.get("grantedScopes")
    }

  private fun appEntityId(appId: String): Long =
    executeInNewTransaction {
      entityManager
        .createQuery("select a.id from App a where a.appId = :appId", java.lang.Long::class.java)
        .setParameter("appId", appId)
        .singleResult
        .toLong()
    }

  private fun appsUrl() = "/v2/organizations/${testData.organization.id}/apps"

  private fun ownedUrl() = "/v2/organizations/${testData.organization.id}/owned-apps"

  private fun mockManifest(json: String) = AppsTestFixtures.mockManifest(appManifestHttpClient, json)

  private fun asAppToken(builder: MockHttpServletRequestBuilder): ResultActions {
    val token = appLevelToken()
    logout()
    val result = perform(builder.header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
    userAccount = testData.user
    return result
  }

  private fun appLevelToken(): String {
    logout()
    val body =
      mapOf(
        "grant_type" to "client_credentials",
        "client_id" to appClientId,
        "client_secret" to appClientSecret,
      )
    val response =
      perform(
        post("/v2/public/apps/token")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(body)),
      ).andIsOk.andReturn().response.contentAsString
    userAccount = testData.user
    return objectMapper.readTree(response).get("access_token").asText()
  }

  private fun manifest(
    id: String = "test-app",
    name: String = "Test App",
    version: String = "0.1.0",
    baseUrl: String = "https://app.example.com",
    scopes: String,
  ): String =
    """
    {
      "id": "$id",
      "name": "$name",
      "version": "$version",
      "baseUrl": "$baseUrl",
      "scopes": [$scopes],
      "modules": {
        "project-dashboard-page": [
          {"key": "home", "title": "Home", "icon": "🏠", "entry": "/"}
        ]
      }
    }
    """.trimIndent()
}
