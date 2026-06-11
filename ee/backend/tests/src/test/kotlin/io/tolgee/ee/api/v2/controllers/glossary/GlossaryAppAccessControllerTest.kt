package io.tolgee.ee.api.v2.controllers.glossary

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.component.KeyGenerator
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.GlossaryAppAccessTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.apps.AppInstall
import io.tolgee.model.enums.Scope
import io.tolgee.repository.apps.AppInstallRepository
import io.tolgee.security.authentication.AppTokenService
import io.tolgee.service.apps.AppEnablementService
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Security matrix for app org-level glossary access (the `@AllowAppAccessWithOrgScope` +
 * [io.tolgee.security.authorization.OrganizationAuthorizationInterceptor] change). Verifies an
 * installed app can read/write glossary terms in its OWN org with the right granted scope — and
 * nothing more: no other org, no missing scope, no non-opted endpoint, no project access, and the
 * existing user/org-role auth on the same endpoints is unchanged.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GlossaryAppAccessControllerTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var appInstallRepository: AppInstallRepository

  @Autowired
  private lateinit var appEnablementService: AppEnablementService

  @Autowired
  private lateinit var appTokenService: AppTokenService

  @Autowired
  private lateinit var keyGenerator: KeyGenerator

  private val jackson = jacksonObjectMapper()

  lateinit var testData: GlossaryAppAccessTestData

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.GLOSSARY)
    testData = GlossaryAppAccessTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  // --- app path: allowed -----------------------------------------------------------------------

  @Test
  fun `app with GLOSSARY_EDIT creates a term in its own org`() {
    val secret = installSecret(testData.organization, setOf(Scope.GLOSSARY_EDIT))
    appPost(termsUrl(testData.organization.id, testData.glossary.id), secret, termBody("Cobalt"))
      .andIsOk
  }

  @Test
  fun `app with GLOSSARY_EDIT adds a term translation in its own org`() {
    val secret = installSecret(testData.organization, setOf(Scope.GLOSSARY_EDIT))
    appPost(
      translationsUrl(testData.organization.id, testData.glossary.id, testData.term.id),
      secret,
      mapOf("languageTag" to "de", "text" to "Kobalt"),
    ).andIsOk
  }

  @Test
  fun `app with GLOSSARY_VIEW reads a term in its own org`() {
    val secret = installSecret(testData.organization, setOf(Scope.GLOSSARY_VIEW))
    appGet(termUrl(testData.organization.id, testData.glossary.id, testData.term.id), secret)
      .andIsOk
  }

  @Test
  fun `app user-context token with GLOSSARY_EDIT creates a term (member user, no maintainer role)`() {
    // testData.user is only an org MEMBER — success here proves the install grant authorizes,
    // not the user's org role.
    val install = installNoCreds(testData.organization, setOf(Scope.GLOSSARY_EDIT))
    appEnablementService.enable(testData.project, install.id, testData.user)
    val token = appTokenService.mintUserContextToken(install.id, testData.user.id, testData.project.id)

    mvc
      .perform(
        post(termsUrl(testData.organization.id, testData.glossary.id))
          .header("Authorization", "Bearer $token")
          .contentType(MediaType.APPLICATION_JSON)
          .content(jackson.writeValueAsString(termBody("Tungsten"))),
      ).andIsOk
  }

  // --- app path: denied ------------------------------------------------------------------------

  @Test
  fun `app without a glossary scope cannot create a term`() {
    val secret = installSecret(testData.organization, setOf(Scope.TRANSLATIONS_VIEW))
    appPost(termsUrl(testData.organization.id, testData.glossary.id), secret, termBody("Cobalt"))
      .andIsForbidden
  }

  @Test
  fun `app cannot create a term in another organization`() {
    // Install lives in org A but targets org B's glossary — cross-org isolation.
    val secret = installSecret(testData.organization, setOf(Scope.GLOSSARY_EDIT))
    appPost(termsUrl(testData.otherOrganization.id, testData.otherGlossary.id), secret, termBody("Cobalt"))
      .andIsForbidden
  }

  @Test
  fun `app with only GLOSSARY_VIEW cannot create a term`() {
    val secret = installSecret(testData.organization, setOf(Scope.GLOSSARY_VIEW))
    appPost(termsUrl(testData.organization.id, testData.glossary.id), secret, termBody("Cobalt"))
      .andIsForbidden
  }

  @Test
  fun `app token is rejected on an org endpoint that did not opt in`() {
    // The org-level glossary LIST is @RequiresOrganizationRole and has no @AllowAppAccessWithOrgScope,
    // so it must still reject app tokens even when they hold glossary scopes.
    val secret = installSecret(testData.organization, setOf(Scope.GLOSSARY_EDIT, Scope.GLOSSARY_VIEW))
    appGet("/v2/organizations/${testData.organization.id}/glossaries", secret)
      .andIsForbidden
  }

  @Test
  fun `app with org glossary scope gets no project permissions`() {
    // Project-level glossary read requires the project scope TRANSLATIONS_VIEW; an org-level
    // glossary scope must not satisfy it.
    val install = installNoCreds(testData.organization, setOf(Scope.GLOSSARY_EDIT))
    val secret = addCredentials(install)
    appEnablementService.enable(testData.project, install.id, testData.user)
    appGet("/v2/projects/${testData.project.id}/glossaries", secret)
      .andIsForbidden
  }

  @Test
  fun `app glossary write is blocked when the GLOSSARY feature is disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    val secret = installSecret(testData.organization, setOf(Scope.GLOSSARY_EDIT))
    appPost(termsUrl(testData.organization.id, testData.glossary.id), secret, termBody("Cobalt"))
      .andExpect(status().isBadRequest)
  }

  // --- regression: user/org-role auth on the same endpoint is unchanged ------------------------

  @Test
  fun `org maintainer user can still create a term`() {
    userAccount = testData.userMaintainer
    performAuthPost(termsUrl(testData.organization.id, testData.glossary.id), termBody("Cobalt"))
      .andIsOk
  }

  @Test
  fun `org member user still cannot create a term`() {
    userAccount = testData.user
    performAuthPost(termsUrl(testData.organization.id, testData.glossary.id), termBody("Cobalt"))
      .andIsForbidden
  }

  // --- helpers ---------------------------------------------------------------------------------

  private fun termsUrl(
    orgId: Long,
    glossaryId: Long,
  ) = "/v2/organizations/$orgId/glossaries/$glossaryId/terms"

  private fun termUrl(
    orgId: Long,
    glossaryId: Long,
    termId: Long,
  ) = "${termsUrl(orgId, glossaryId)}/$termId"

  private fun translationsUrl(
    orgId: Long,
    glossaryId: Long,
    termId: Long,
  ) = "${termUrl(orgId, glossaryId, termId)}/translations"

  private fun termBody(text: String) = mapOf("text" to text, "description" to "added by app")

  private fun appPost(
    url: String,
    secret: String,
    body: Any,
  ): ResultActions =
    mvc.perform(
      post(url)
        .header("X-API-Key", secret)
        .contentType(MediaType.APPLICATION_JSON)
        .content(jackson.writeValueAsString(body)),
    )

  private fun appGet(
    url: String,
    secret: String,
  ): ResultActions = mvc.perform(get(url).header("X-API-Key", secret))

  private fun installNoCreds(
    org: io.tolgee.model.Organization,
    scopes: Set<Scope>,
  ): AppInstall {
    val appId = "glossary-keeper-${System.nanoTime()}"
    val install =
      AppInstall().apply {
        this.organization = org
        this.author = testData.user
        this.manifestUrl = "https://example.com/manifest.json"
        this.appId = appId
        this.name = "Glossary Keeper"
        this.version = "0.1.0"
        this.baseUrl = "https://app.example.com"
        this.manifestJson = manifestJsonFor(appId)
        this.grantedScopes = scopes.toMutableSet()
      }
    return appInstallRepository.save(install)
  }

  private fun addCredentials(install: AppInstall): String {
    val plaintextSecret = "tgapps_${keyGenerator.generate(256)}"
    install.clientId = "tgapp_${keyGenerator.generate(128)}"
    install.clientSecretHash = keyGenerator.hash(plaintextSecret)
    install.clientSecretPrefix = plaintextSecret.take(10)
    install.webhookSecret = "tgappw_${keyGenerator.generate(256)}"
    appInstallRepository.save(install)
    return plaintextSecret
  }

  private fun installSecret(
    org: io.tolgee.model.Organization,
    scopes: Set<Scope>,
  ): String = addCredentials(installNoCreds(org, scopes))

  private fun manifestJsonFor(appId: String): String =
    """
    {
      "id": "$appId",
      "name": "Glossary Keeper",
      "version": "0.1.0",
      "baseUrl": "https://app.example.com",
      "modules": {"project-dashboard-page": []}
    }
    """.trimIndent()
}
