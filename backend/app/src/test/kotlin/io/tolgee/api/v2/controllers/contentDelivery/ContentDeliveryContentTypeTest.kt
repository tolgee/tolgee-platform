package io.tolgee.api.v2.controllers.contentDelivery

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobConcurrentLauncher
import io.tolgee.component.fileStorage.AzureFileStorageFactory
import io.tolgee.development.testDataBuilder.data.ContentDeliveryContentTypeTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.getStoreFileCalls
import io.tolgee.fixtures.mockCreatedStorage
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

class ContentDeliveryContentTypeTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: ContentDeliveryContentTypeTestData

  @Autowired
  @MockitoSpyBean
  private lateinit var azureFileStorageFactory: AzureFileStorageFactory

  @Autowired
  private lateinit var batchJobConcurrentLauncher: BatchJobConcurrentLauncher

  @BeforeEach
  fun setup() {
    batchJobConcurrentLauncher.pause = true
    testData = ContentDeliveryContentTypeTestData()
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    Mockito.reset(azureFileStorageFactory)
  }

  @AfterEach
  fun after() {
    testDataService.cleanTestData(testData.root)
    batchJobConcurrentLauncher.pause = false
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `publishes each file with its own content type`() {
    val mocked = azureFileStorageFactory.mockCreatedStorage()

    performProjectAuthPost(
      "content-delivery-configs/${testData.appleContentDeliveryConfig.self.id}",
    ).andIsOk

    val stored = mocked.getStoreFileCalls()
    stored.assert.hasSize(2)
    stored
      .single { it.path.endsWith(".strings") }
      .contentType.assert
      .isEqualTo("text/plain; charset=UTF-8")
    stored
      .single { it.path.endsWith(".stringsdict") }
      .contentType.assert
      .isEqualTo("application/xml")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `publishes without a content type when the template renders the extension unseparated`() {
    val mocked = azureFileStorageFactory.mockCreatedStorage()

    performProjectAuthPost(
      "content-delivery-configs/${testData.unrecoverableExtensionContentDeliveryConfig.self.id}",
    ).andIsOk

    val stored = mocked.getStoreFileCalls()
    stored.assert.hasSize(2)
    stored.forEach { it.contentType.assert.isNull() }
  }
}
