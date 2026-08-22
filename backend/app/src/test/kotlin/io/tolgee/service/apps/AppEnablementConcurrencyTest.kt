package io.tolgee.service.apps

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.bean.override.mockito.MockitoBean

class AppEnablementConcurrencyTest : AbstractSpringTest() {
  @Autowired
  private lateinit var appInstallService: AppInstallService

  @Autowired
  private lateinit var appEnablementService: AppEnablementService

  @Autowired
  private lateinit var appEnablementInserter: AppEnablementInserter

  @MockitoBean
  @Autowired
  private lateinit var appManifestHttpClient: AppManifestHttpClient

  private lateinit var testData: AppsTestData
  private var installId: Long = 0

  @BeforeEach
  fun setup() {
    AppsTestFixtures.mockManifest(appManifestHttpClient)
    testData = AppsTestData()
    testDataService.saveTestData(testData.root)
    installId =
      executeInNewTransaction(platformTransactionManager) {
        appInstallService
          .register(
            organization = testData.organization,
            manifestUrl = AppsTestFixtures.MANIFEST_URL,
            manifestHash = null,
            install = true,
          ).install!!
          .id
      }
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `a duplicate enablement insert does not doom the calling transaction`() {
    executeInNewTransaction(platformTransactionManager) {
      appEnablementInserter.insert(installId, testData.project.id)

      assertThrows<DataIntegrityViolationException> {
        appEnablementInserter.insert(installId, testData.project.id)
      }

      appInstallService.findAll(testData.organization.id).assert.hasSize(1)
    }
  }

  @Test
  fun `enable stays idempotent when the enablement already exists`() {
    executeInNewTransaction(platformTransactionManager) {
      appEnablementService.enable(testData.projectBuilder.self, installId)
    }
    executeInNewTransaction(platformTransactionManager) {
      appEnablementService.enable(testData.projectBuilder.self, installId)
    }

    executeInNewTransaction(platformTransactionManager) {
      appEnablementService
        .listAppsForProject(testData.projectBuilder.self)
        .single()
        .enabled.assert
        .isEqualTo(true)
    }
  }
}
