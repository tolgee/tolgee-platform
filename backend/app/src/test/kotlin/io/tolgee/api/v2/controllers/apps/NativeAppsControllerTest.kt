package io.tolgee.api.v2.controllers.apps

import io.tolgee.development.testDataBuilder.data.LateAppsOrganizationTestData
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

  var lateTestData: LateAppsOrganizationTestData? = null

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
    lateTestData?.let { testDataService.cleanTestData(it.root) }
    lateTestData = null
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
  fun `registers a native app from a manifest URL, disclosing the client secret once`() {
    performAuthPost("/v2/administration/apps", registerBody()).andIsOk.andAssertThatJson {
      node("appId").isEqualTo("test-app")
      node("name").isEqualTo("Test App")
      node("baseUrl").isEqualTo("https://app.example.com")
      node("availableToAllOrganizations").isEqualTo(false)
      node("clientId").isString.startsWith(AppInstallService.CLIENT_ID_PREFIX)
      node("clientSecret").isString.startsWith(AppInstallService.CLIENT_SECRET_PREFIX)
    }

    val install = AppsTestFixtures.nativeInstalls(appInstallService).single()
    install.organization.assert.isNull()
    install.author.id.assert.isEqualTo(testData.admin.id)

    performAuthGet("/v2/administration/apps").andIsOk.andAssertThatJson {
      node("_embedded.appInstalls").isArray.hasSize(1)
      node("_embedded.appInstalls[0].clientSecret").isNull()
    }
  }

  @Test
  fun `previews a manifest without registering anything`() {
    performAuthPost("/v2/administration/apps/preview", registerBody()).andIsOk.andAssertThatJson {
      node("appId").isEqualTo("test-app")
      node("name").isEqualTo("Test App")
      node("version").isEqualTo("0.1.0")
      node("baseUrl").isEqualTo("https://app.example.com")
      node("modules.project-dashboard-page[0].title").isEqualTo("Home")
      node("requestedScopes").isArray.isEmpty()
    }

    AppsTestFixtures.nativeInstalls(appInstallService).assert.isEmpty()
  }

  @Test
  fun `registering the same manifest twice is refused and leaves the first install alone`() {
    performAuthPost("/v2/administration/apps", registerBody()).andIsOk
    val install = AppsTestFixtures.nativeInstalls(appInstallService).single()

    performAuthPost("/v2/administration/apps", registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_already_installed")
    }

    AppsTestFixtures.nativeInstalls(appInstallService).map { it.id }.assert.containsExactly(install.id)
  }

  @Test
  fun `an organization-owned install with the same app id does not block a native registration`() {
    registerOrganizationApp()
    AppsTestFixtures.mockManifest(appManifestHttpClient)

    performAuthPost("/v2/administration/apps", registerBody()).andIsOk

    AppsTestFixtures.nativeInstalls(appInstallService).assert.hasSize(1)
  }

  @Test
  fun `the preview and register endpoints reject a non-admin caller`() {
    userAccount = testData.user

    performAuthPost("/v2/administration/apps/preview", registerBody()).andIsForbidden
    performAuthPost("/v2/administration/apps", registerBody()).andIsForbidden

    userAccount = testData.admin
    AppsTestFixtures.nativeInstalls(appInstallService).assert.isEmpty()
  }

  @Test
  fun `the preview and register endpoints reject a supporter`() {
    userAccount = testData.supporter

    performAuthPost("/v2/administration/apps/preview", registerBody()).andIsForbidden
    performAuthPost("/v2/administration/apps", registerBody()).andIsForbidden

    userAccount = testData.admin
    AppsTestFixtures.nativeInstalls(appInstallService).assert.isEmpty()
  }

  @Test
  fun `a project can enable a natively registered app once it is available to its organization`() {
    performAuthPost("/v2/administration/apps", registerBody()).andIsOk
    val install = AppsTestFixtures.nativeInstalls(appInstallService).single()
    grantAvailability(install)

    userAccount = testData.user
    performAuthPut("${projectAppsUrl()}/${install.id}", null).andIsOk

    appEnablementService.isEnabledForProject(testData.project.id, install.id).assert.isTrue()
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
    performAuthPut(allOrganizationsUrl(install), null).andIsForbidden
    performAuthDelete(allOrganizationsUrl(install)).andIsForbidden
    performAuthDelete(appsUrl(install)).andIsForbidden
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

  @Test
  fun `a project can enable a native app made available to all organizations without an explicit grant`() {
    val install = createNativeInstall()

    performAuthPut(allOrganizationsUrl(install), null).andIsOk

    appAvailabilityService.listOrganizations(install.id).assert.isEmpty()
    userAccount = testData.user
    performAuthPut("${projectAppsUrl()}/${install.id}", null).andIsOk.andAssertThatJson {
      node("appId").isEqualTo("test-app")
      node("enabled").isEqualTo(true)
    }
    appEnablementService.isEnabledForProject(testData.project.id, install.id).assert.isTrue()
  }

  @Test
  fun `the project listing shows a native app made available to all organizations`() {
    val install = createNativeInstall()
    performAuthPut(allOrganizationsUrl(install), null).andIsOk
    userAccount = testData.user

    performAuthGet(projectAppsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.projectApps").isArray.hasSize(1)
      node("_embedded.projectApps[0].id").isEqualTo(install.id)
    }
  }

  @Test
  fun `the admin listing exposes the all-organizations flag`() {
    val install = createNativeInstall()

    performAuthGet("/v2/administration/apps").andIsOk.andAssertThatJson {
      node("_embedded.appInstalls[0].availableToAllOrganizations").isEqualTo(false)
    }

    performAuthPut(allOrganizationsUrl(install), null).andIsOk

    performAuthGet("/v2/administration/apps").andIsOk.andAssertThatJson {
      node("_embedded.appInstalls[0].availableToAllOrganizations").isEqualTo(true)
    }
  }

  @Test
  fun `an organization created after the flag was set is covered as well`() {
    val install = createNativeInstall()
    performAuthPut(allOrganizationsUrl(install), null).andIsOk

    lateTestData = LateAppsOrganizationTestData()
    testDataService.saveTestData(lateTestData!!.root)
    val late = lateTestData!!

    val available = appAvailabilityService.listNativeInstallsForOrganization(late.organization.id)
    available.map { it.id }.assert.containsExactly(install.id)
    userAccount = late.user
    performAuthPut("/v2/projects/${late.project.id}/apps/${install.id}", null).andIsOk
    appEnablementService.isEnabledForProject(late.project.id, install.id).assert.isTrue()
  }

  @Test
  fun `granting to all organizations is idempotent and keeps explicit grants intact`() {
    val install = createNativeInstall()
    grantAvailability(install)

    performAuthPut(allOrganizationsUrl(install), null).andIsOk
    performAuthPut(allOrganizationsUrl(install), null).andIsOk

    appAvailabilityService.listOrganizations(install.id).assert.hasSize(1)
    nativeInstall(install).availableToAllOrganizations.assert.isTrue()
  }

  @Test
  fun `revoking from all organizations is idempotent`() {
    val install = createNativeInstall()

    performAuthDelete(allOrganizationsUrl(install)).andIsOk
    performAuthPut(allOrganizationsUrl(install), null).andIsOk
    performAuthDelete(allOrganizationsUrl(install)).andIsOk
    performAuthDelete(allOrganizationsUrl(install)).andIsOk

    nativeInstall(install).availableToAllOrganizations.assert.isFalse()
  }

  @Test
  fun `revoking from all organizations disables the app only where it was not explicitly granted`() {
    val install = createNativeInstall()
    performAuthPut(allOrganizationsUrl(install), null).andIsOk
    grantAvailability(install, testData.otherOrganization.id)
    userAccount = testData.user
    performAuthPut("${projectAppsUrl()}/${install.id}", null).andIsOk
    userAccount = testData.otherOwner
    performAuthPut("/v2/projects/${testData.otherProject.id}/apps/${install.id}", null).andIsOk

    userAccount = testData.admin
    performAuthDelete(allOrganizationsUrl(install)).andIsOk

    appEnablementService.isEnabledForProject(testData.project.id, install.id).assert.isFalse()
    appEnablementService.isEnabledForProject(testData.otherProject.id, install.id).assert.isTrue()
  }

  @Test
  fun `granting and revoking a single organization leaves the all-organizations flag alone`() {
    val install = createNativeInstall()
    performAuthPut(allOrganizationsUrl(install), null).andIsOk

    performAuthPut(organizationUrl(install), null).andIsOk
    nativeInstall(install).availableToAllOrganizations.assert.isTrue()
    userAccount = testData.user
    performAuthPut("${projectAppsUrl()}/${install.id}", null).andIsOk

    userAccount = testData.admin
    performAuthDelete(organizationUrl(install)).andIsOk

    nativeInstall(install).availableToAllOrganizations.assert.isTrue()
    appAvailabilityService.listOrganizations(install.id).assert.isEmpty()
    appEnablementService.isEnabledForProject(testData.project.id, install.id).assert.isTrue()
  }

  @Test
  fun `deregistering a native app removes it everywhere`() {
    val install = createNativeInstall()
    performAuthPut(allOrganizationsUrl(install), null).andIsOk
    grantAvailability(install, testData.otherOrganization.id)
    userAccount = testData.user
    performAuthPut("${projectAppsUrl()}/${install.id}", null).andIsOk
    userAccount = testData.otherOwner
    performAuthPut("/v2/projects/${testData.otherProject.id}/apps/${install.id}", null).andIsOk

    userAccount = testData.admin
    performAuthDelete(appsUrl(install)).andIsOk

    AppsTestFixtures.nativeInstalls(appInstallService).assert.isEmpty()
    appAvailabilityService.listOrganizations(install.id).assert.isEmpty()
    appEnablementService.isEnabledForProject(testData.project.id, install.id).assert.isFalse()
    appEnablementService.isEnabledForProject(testData.otherProject.id, install.id).assert.isFalse()
  }

  @Test
  fun `the all-organizations and deregister endpoints reject an organization-owned install`() {
    val install = registerOrganizationApp()

    performAuthPut(allOrganizationsUrl(install), null).andIsNotFound
    performAuthDelete(allOrganizationsUrl(install)).andIsNotFound
    performAuthDelete(appsUrl(install)).andIsNotFound

    appInstallService.find(testData.organization.id, install.id).assert.isNotNull
  }

  @Test
  fun `the all-organizations and deregister endpoints reject a supporter`() {
    val install = createNativeInstall()
    userAccount = testData.supporter

    performAuthPut(allOrganizationsUrl(install), null).andIsForbidden
    performAuthDelete(allOrganizationsUrl(install)).andIsForbidden
    performAuthDelete(appsUrl(install)).andIsForbidden

    userAccount = testData.admin
    nativeInstall(install).availableToAllOrganizations.assert.isFalse()
  }

  private fun nativeInstall(install: AppInstall) = appInstallService.getNative(install.id)

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

  private fun registerBody() = mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL)

  private fun appsUrl(install: AppInstall) = "/v2/administration/apps/${install.id}"

  private fun organizationUrl(
    install: AppInstall,
    organizationId: Long = testData.organization.id,
  ) = "${appsUrl(install)}/organizations/$organizationId"

  private fun allOrganizationsUrl(install: AppInstall) = "${appsUrl(install)}/organizations/all"

  private fun projectAppsUrl() = "/v2/projects/${testData.project.id}/apps"

  companion object {
    private val ORGANIZATION_MANIFEST =
      AppsTestFixtures.MANIFEST.replace("\"test-app\"", "\"org-app\"")
  }
}
