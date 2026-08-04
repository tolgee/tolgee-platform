package io.tolgee.api.v2.controllers.apps

import io.tolgee.development.testDataBuilder.data.NativeAppsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.apps.AppInstall
import io.tolgee.service.apps.AppAvailabilityService
import io.tolgee.service.apps.AppEnablementService
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.service.apps.AppsTestFixtures
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean

class NativeAppsControllerTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appInstallService: AppInstallService

  @Autowired
  lateinit var appAvailabilityService: AppAvailabilityService

  @Autowired
  lateinit var appEnablementService: AppEnablementService

  @MockitoBean
  @Autowired
  lateinit var appManifestHttpClient: AppManifestHttpClient

  lateinit var testData: NativeAppsTestData

  @BeforeEach
  fun setup() {
    testData = NativeAppsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.admin
    AppsTestFixtures.mockManifest(appManifestHttpClient)
  }

  @AfterEach
  fun cleanup() {
    AppsTestFixtures.removeNativeInstalls(appInstallService)
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `lists native installs without ever disclosing the client secret`() {
    createNativeInstall()

    performAuthGet("/v2/administration/apps").andIsOk.andAssertThatJson {
      node("_embedded.appInstalls").isArray.hasSize(1)
      node("_embedded.appInstalls[0].appId").isEqualTo("test-app")
      node("_embedded.appInstalls[0].clientId").isString.startsWith(AppInstallService.CLIENT_ID_PREFIX)
      node("_embedded.appInstalls[0].clientSecret").isNull()
    }
  }

  @Test
  fun `does not list an organization-owned install among the native ones`() {
    registerOrganizationApp()

    performAuthGet("/v2/administration/apps").andIsOk.andAssertThatJson {
      node("_embedded.appInstalls").isAbsent()
    }
  }

  @Test
  fun `grants, lists and revokes availability for an organization`() {
    val install = createNativeInstall()

    performAuthGet("${appsUrl(install)}/organizations").andIsOk.andAssertThatJson {
      node("_embedded.organizations").isAbsent()
    }

    performAuthPut(organizationUrl(install), null).andIsOk

    performAuthGet("${appsUrl(install)}/organizations").andIsOk.andAssertThatJson {
      node("_embedded.organizations").isArray.hasSize(1)
      node("_embedded.organizations[0].id").isEqualTo(testData.organization.id)
      node("_embedded.organizations[0].slug").isEqualTo(testData.organization.slug)
      node("_embedded.organizations[0].name").isEqualTo(testData.organization.name)
    }

    performAuthDelete(organizationUrl(install)).andIsOk

    performAuthGet("${appsUrl(install)}/organizations").andIsOk.andAssertThatJson {
      node("_embedded.organizations").isAbsent()
    }
  }

  @Test
  fun `grant and revoke are idempotent`() {
    val install = createNativeInstall()

    performAuthPut(organizationUrl(install), null).andIsOk
    performAuthPut(organizationUrl(install), null).andIsOk
    appAvailabilityService.listOrganizations(install.id).assert.hasSize(1)

    performAuthDelete(organizationUrl(install)).andIsOk
    performAuthDelete(organizationUrl(install)).andIsOk
    appAvailabilityService.listOrganizations(install.id).assert.isEmpty()
  }

  @Test
  fun `availability cannot be granted for an organization-owned install`() {
    val install = registerOrganizationApp()

    performAuthGet("${appsUrl(install)}/organizations").andIsNotFound
    performAuthPut(organizationUrl(install), null).andIsNotFound
  }

  @Test
  fun `revoking against an organization-owned install leaves its project enablements alone`() {
    val install = registerOrganizationApp()
    userAccount = testData.user
    performAuthPut("${projectAppsUrl()}/${install.id}", null).andIsOk

    userAccount = testData.admin
    performAuthDelete(organizationUrl(install)).andIsOk

    appEnablementService.isEnabledForProject(testData.project.id, install.id).assert.isTrue()
  }

  @Test
  fun `every administration apps endpoint rejects a non-admin caller`() {
    val install = createNativeInstall()
    userAccount = testData.user

    performAuthGet("/v2/administration/apps").andIsForbidden
    performAuthGet("${appsUrl(install)}/organizations").andIsForbidden
    performAuthPut(organizationUrl(install), null).andIsForbidden
    performAuthDelete(organizationUrl(install)).andIsForbidden
  }

  @Test
  fun `a project cannot enable a native app before it is available to its organization`() {
    val install = createNativeInstall()
    userAccount = testData.user

    performAuthPut("${projectAppsUrl()}/${install.id}", null).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_not_available_for_organization")
    }
    appEnablementService.isEnabledForProject(testData.project.id, install.id).assert.isFalse()
  }

  @Test
  fun `a project can enable a native app once it is available to its organization`() {
    val install = createNativeInstall()
    grantAvailability(install)
    userAccount = testData.user

    performAuthPut("${projectAppsUrl()}/${install.id}", null).andIsOk.andAssertThatJson {
      node("appId").isEqualTo("test-app")
      node("enabled").isEqualTo(true)
    }
    appEnablementService.isEnabledForProject(testData.project.id, install.id).assert.isTrue()
  }

  @Test
  fun `the project listing shows an available native app alongside the organization's own`() {
    registerOrganizationApp()
    val nativeInstall = createNativeInstall()
    grantAvailability(nativeInstall)
    userAccount = testData.user

    performAuthGet(projectAppsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.projectApps").isArray.hasSize(2)
      node("_embedded.projectApps[0].appId").isEqualTo("org-app")
      node("_embedded.projectApps[1].appId").isEqualTo("test-app")
      node("_embedded.projectApps[1].id").isEqualTo(nativeInstall.id)
    }
  }

  @Test
  fun `the project listing hides a native app that is not available to the organization`() {
    createNativeInstall()
    userAccount = testData.user

    performAuthGet(projectAppsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.projectApps").isAbsent()
    }
  }

  @Test
  fun `revoking availability disables the app in the organization's already-enabled projects`() {
    val install = createNativeInstall()
    grantAvailability(install)
    userAccount = testData.user
    performAuthPut("${projectAppsUrl()}/${install.id}", null).andIsOk
    performAuthPut("/v2/projects/${testData.siblingProject.id}/apps/${install.id}", null).andIsOk

    userAccount = testData.admin
    performAuthDelete(organizationUrl(install)).andIsOk

    appEnablementService.isEnabledForProject(testData.project.id, install.id).assert.isFalse()
    appEnablementService.isEnabledForProject(testData.siblingProject.id, install.id).assert.isFalse()
  }

  @Test
  fun `revoking availability for one organization leaves another organization's projects untouched`() {
    val install = createNativeInstall()
    grantAvailability(install)
    grantAvailability(install, testData.otherOrganization.id)
    userAccount = testData.user
    performAuthPut("${projectAppsUrl()}/${install.id}", null).andIsOk
    userAccount = testData.otherOwner
    performAuthPut("/v2/projects/${testData.otherProject.id}/apps/${install.id}", null).andIsOk

    userAccount = testData.admin
    performAuthDelete(organizationUrl(install)).andIsOk

    appEnablementService.isEnabledForProject(testData.project.id, install.id).assert.isFalse()
    appEnablementService.isEnabledForProject(testData.otherProject.id, install.id).assert.isTrue()
  }

  @Test
  fun `removing a native install clears its availability rows`() {
    val install = createNativeInstall()
    grantAvailability(install)

    appInstallService.remove(organizationId = null, installId = install.id)

    appAvailabilityService.listOrganizations(install.id).assert.isEmpty()
    appAvailabilityService.listNativeInstallsForOrganization(testData.organization.id).assert.isEmpty()
  }

  private fun createNativeInstall(): AppInstall {
    AppsTestFixtures.mockManifest(appManifestHttpClient)
    return appInstallService
      .selfRegister(
        organization = null,
        manifestUrl = AppsTestFixtures.MANIFEST_URL,
        author = testData.user,
      ).install
  }

  private fun registerOrganizationApp(): AppInstall {
    AppsTestFixtures.mockManifest(appManifestHttpClient, ORGANIZATION_MANIFEST)
    return appInstallService
      .register(
        organization = testData.organization,
        manifestUrl = AppsTestFixtures.MANIFEST_URL,
        author = testData.user,
      ).install
  }

  private fun grantAvailability(
    install: AppInstall,
    organizationId: Long = testData.organization.id,
  ) {
    appAvailabilityService.grant(
      installId = install.id,
      organizationId = organizationId,
      author = testData.admin,
    )
  }

  private fun appsUrl(install: AppInstall) = "/v2/administration/apps/${install.id}"

  private fun organizationUrl(
    install: AppInstall,
    organizationId: Long = testData.organization.id,
  ) = "${appsUrl(install)}/organizations/$organizationId"

  private fun projectAppsUrl() = "/v2/projects/${testData.project.id}/apps"

  companion object {
    private val ORGANIZATION_MANIFEST =
      AppsTestFixtures.MANIFEST.replace("\"test-app\"", "\"org-app\"")
  }
}
