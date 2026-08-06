package io.tolgee.service.apps

import io.tolgee.configuration.tolgee.AppsProperties
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.NativeAppsTestData
import io.tolgee.exceptions.BadRequestException
import io.tolgee.fixtures.EmailTestUtil
import io.tolgee.model.apps.AppManifestFailureKind
import io.tolgee.repository.apps.AppRepository
import io.tolgee.service.apps.lifecycle.AppLifecycleHttpClient
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Duration

/**
 * The one part of the app layer that destroys other people's installs on a timer, so every test here
 * is about what must *not* happen: one failure reaps nothing, a manifest that is served but invalid
 * is never reaped, and a recovery wipes the state clean.
 *
 * The clock is driven through [io.tolgee.component.CurrentDateProvider]; nothing waits.
 */
class AppManifestReaperTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appManifestReaper: AppManifestReaper

  @Autowired
  lateinit var appsProperties: AppsProperties

  @Autowired
  lateinit var appInstallService: AppInstallService

  @Autowired
  lateinit var appRepository: AppRepository

  @Autowired
  lateinit var emailTestUtil: EmailTestUtil

  @MockitoBean
  @Autowired
  lateinit var appManifestHttpClient: AppManifestHttpClient

  @MockitoBean
  @Autowired
  lateinit var appLifecycleHttpClient: AppLifecycleHttpClient

  lateinit var testData: NativeAppsTestData
  var appEntityId: Long = 0

  @BeforeEach
  fun setup() {
    testData = NativeAppsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    AppsTestFixtures.mockManifest(appManifestHttpClient)
    emailTestUtil.initMocks()
    setForcedDate()

    appEntityId =
      executeInNewTransaction {
        appInstallService
          .register(
            organization = testData.organization,
            manifestUrl = AppsTestFixtures.MANIFEST_URL,
            author = testData.user,
          ).app.id
      }
    appsProperties.reapUnreachableApps = true
  }

  @AfterEach
  fun cleanup() {
    appsProperties.reapUnreachableApps = false
    clearForcedDate()
    AppsTestFixtures.removeNativeInstalls(appInstallService)
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `one failure records the error but never marks the app unhealthy`() {
    failManifest()

    appManifestReaper.check(appEntityId)

    val app = reload()
    app.manifestFailureCount.assert.isEqualTo(1)
    app.manifestFirstFailedAt.assert.isNotNull
    app.manifestLastError.assert.contains("connection refused")
    app.manifestLastFailureKind.assert.isEqualTo(AppManifestFailureKind.UNREACHABLE)
    app.unhealthySince.assert.isNull()
    emailTestUtil.messageContents.assert.isEmpty()
  }

  /** Failing many times inside a few seconds is one outage, not a sustained one. */
  @Test
  fun `a burst of failures inside the window does not mark the app unhealthy`() {
    failManifest()

    repeat(10) { appManifestReaper.check(appEntityId) }

    reload().unhealthySince.assert.isNull()
  }

  @Test
  fun `sustained failure marks the app unhealthy and notifies its owner exactly once`() {
    failUntilUnhealthy()

    val app = reload()
    app.unhealthySince.assert.isNotNull
    app.unhealthyNotifiedAt.assert.isNotNull
    emailTestUtil.assertEmailTo.isEqualTo(testData.user.username)

    repeat(3) { appManifestReaper.check(appEntityId) }
    emailTestUtil.verifyTimesEmailSent(1)
  }

  @Test
  fun `recovering before the grace period runs out clears the state`() {
    failUntilUnhealthy()

    AppsTestFixtures.mockManifest(appManifestHttpClient)
    appManifestReaper.check(appEntityId)

    val app = reload()
    app.unhealthySince.assert.isNull()
    app.manifestFailureCount.assert.isEqualTo(0)
    app.manifestFirstFailedAt.assert.isNull()
    app.manifestLastError.assert.isNull()

    moveCurrentDate(Duration.ofDays(365))
    appManifestReaper.check(appEntityId)
    appInstallService.findAll(testData.organization.id).assert.hasSize(1)
  }

  @Test
  fun `the app is removed only once the grace period has passed`() {
    failUntilUnhealthy()

    moveCurrentDate(Duration.ofDays(appsProperties.manifestReapAfterUnhealthyDays - 1))
    appManifestReaper.check(appEntityId)
    appInstallService.findAll(testData.organization.id).assert.hasSize(1)

    moveCurrentDate(Duration.ofDays(2))
    appManifestReaper.check(appEntityId)

    appInstallService.findAll(testData.organization.id).assert.isEmpty()
    executeInNewTransaction { appRepository.findByAppId("test-app").assert.isNull() }
  }

  /** Somebody is still serving the document, so its author is there to fix it. */
  @Test
  fun `a manifest that is served but no longer valid is never reaped`() {
    AppsTestFixtures.mockManifest(appManifestHttpClient, """{"id":"test-app"}""")
    failUntilUnhealthy(refetch = false)

    reload().manifestLastFailureKind.assert.isEqualTo(AppManifestFailureKind.INVALID)

    moveCurrentDate(Duration.ofDays(appsProperties.manifestReapAfterUnhealthyDays + 1))
    appManifestReaper.check(appEntityId)

    appInstallService.findAll(testData.organization.id).assert.hasSize(1)
  }

  @Test
  fun `reaping stays off unless it is switched on`() {
    appsProperties.reapUnreachableApps = false
    failUntilUnhealthy()

    moveCurrentDate(Duration.ofDays(appsProperties.manifestReapAfterUnhealthyDays + 1))
    appManifestReaper.check(appEntityId)

    appInstallService.findAll(testData.organization.id).assert.hasSize(1)
    reload().unhealthySince.assert.isNotNull
  }

  private fun failManifest() {
    doThrow(BadRequestException(Message.APP_MANIFEST_FETCH_FAILED, listOf("connection refused")))
      .whenever(appManifestHttpClient)
      .fetchBody(anyString())
  }

  /** Leaves the clock at the moment the app was marked unhealthy, so the grace period starts now. */
  private fun failUntilUnhealthy(refetch: Boolean = true) {
    if (refetch) failManifest()
    appManifestReaper.check(appEntityId)
    moveCurrentDate(Duration.ofHours(appsProperties.manifestUnhealthyAfterHours))
    repeat(appsProperties.manifestUnhealthyMinFailures) { appManifestReaper.check(appEntityId) }
  }

  private fun reload() = executeInNewTransaction { appRepository.findById(appEntityId).orElseThrow() }
}
