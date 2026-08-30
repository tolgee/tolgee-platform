package io.tolgee.service.apps

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.AppsWithInstallsTestData
import io.tolgee.repository.apps.AppEnabledForProjectRepository
import io.tolgee.service.apps.lifecycle.AppLifecycleHttpClient
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ],
)
class AppEnablementCacheTest : AbstractSpringTest() {
  @MockitoSpyBean
  @Autowired
  private lateinit var appEnabledForProjectRepository: AppEnabledForProjectRepository

  @Autowired
  private lateinit var appEnablementService: AppEnablementService

  @MockitoBean
  @Autowired
  private lateinit var appManifestHttpClient: AppManifestHttpClient

  @MockitoBean
  @Autowired
  private lateinit var appLifecycleHttpClient: AppLifecycleHttpClient

  private lateinit var testData: AppsWithInstallsTestData

  @BeforeEach
  fun setup() {
    testData = AppsWithInstallsTestData()
    testDataService.saveTestData(testData.root)
    clearCaches()
    Mockito.reset(appEnabledForProjectRepository)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `serves the enablement check from cache on the second call`() {
    val projectId = testData.projectBuilder.self.id
    val installId = testData.enabledInstall.id

    appEnablementService.isEnabledForProject(projectId, installId).assert.isEqualTo(true)
    appEnablementService.isEnabledForProject(projectId, installId).assert.isEqualTo(true)

    verify(appEnabledForProjectRepository, times(1)).findEnabledProjectIdsByInstallId(installId)
  }

  @Test
  fun `disable evicts so the check does not read stale as enabled`() {
    val projectId = testData.projectBuilder.self.id
    val installId = testData.enabledInstall.id
    appEnablementService.isEnabledForProject(projectId, installId).assert.isEqualTo(true)

    appEnablementService.disable(projectId, installId)

    appEnablementService.isEnabledForProject(projectId, installId).assert.isEqualTo(false)
  }

  @Test
  fun `uninstall evicts so the check does not read stale as enabled`() {
    val projectId = testData.projectBuilder.self.id
    val installId = testData.enabledInstall.id
    appEnablementService.isEnabledForProject(projectId, installId).assert.isEqualTo(true)

    appEnablementService.removeAllForAppInstall(installId)

    appEnablementService.isEnabledForProject(projectId, installId).assert.isEqualTo(false)
  }

  @Test
  fun `enable evicts the negative entry so the check reads enabled immediately`() {
    val siblingProject = testData.siblingProject
    val installId = testData.enabledInstall.id
    appEnablementService.isEnabledForProject(siblingProject.id, installId).assert.isEqualTo(false)

    appEnablementService.enable(siblingProject, installId)

    appEnablementService.isEnabledForProject(siblingProject.id, installId).assert.isEqualTo(true)
  }
}
