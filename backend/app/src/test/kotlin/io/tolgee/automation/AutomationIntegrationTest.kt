package io.tolgee.automation

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.component.contentDelivery.ContentDeliveryFileStorageProvider
import io.tolgee.component.contentDelivery.cachePurging.ContentDeliveryCachePurging
import io.tolgee.component.contentDelivery.cachePurging.ContentDeliveryCachePurgingProvider
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.development.testDataBuilder.data.ContentDeliveryConfigTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.service.contentDelivery.ContentDeliveryConfigService
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.invocation.Invocation
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class AutomationIntegrationTest : ProjectAuthControllerTest("/v2/projects/") {
  @MockitoBean
  @Autowired
  lateinit var contentDeliveryFileStorageProvider: ContentDeliveryFileStorageProvider

  lateinit var fileStorageMock: FileStorage

  @MockitoBean
  @Autowired
  lateinit var contentDeliveryCachePurgingProvider: ContentDeliveryCachePurgingProvider

  lateinit var purgingMock: ContentDeliveryCachePurging

  @Autowired
  lateinit var contentDeliveryConfigService: ContentDeliveryConfigService

  @AfterEach
  fun after() {
    currentDateProvider.forcedDate = null
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `publishes to Content Delivery`() {
    currentDateProvider.forcedDate = currentDateProvider.date
    val testData = ContentDeliveryConfigTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.projectBuilder.self }
    fileStorageMock = mock()
    doReturn(fileStorageMock).whenever(contentDeliveryFileStorageProvider).getContentStorageWithDefaultClient()
    purgingMock = mock()
    doReturn(listOf(purgingMock)).whenever(contentDeliveryCachePurgingProvider).purgings

    // wait for the first invocation happening because of test data saving, then clear invocations
    Thread.sleep(1000)
    Mockito.clearInvocations(fileStorageMock)

    modifyTranslationData()
    verifyContentDeliveryPublish()

    waitForNotThrowing(pollTime = 200) {
      contentDeliveryConfigService
        .get(testData.defaultServerContentDeliveryConfig.self.id)
        .lastPublished!!
        .time
        .assert
        .isEqualTo(currentDateProvider.date.time)
    }
  }

  private fun verifyContentDeliveryPublish() {
    waitForNotThrowing(timeout = 2000) {
      verify(fileStorageMock, times(1)).storeFile(any(), any())
      val storeFileInvocations =
        fileStorageInvocations.filter { it.method.name == "storeFile" }
      storeFileInvocations.assert.hasSize(1)
      val pruneDirectoryInvocations =
        fileStorageInvocations.filter { it.method.name == "pruneDirectory" }
      pruneDirectoryInvocations.assert.hasSize(1)
    }
  }

  private val fileStorageInvocations: MutableCollection<Invocation>
    get() = Mockito.mockingDetails(fileStorageMock).invocations

  private fun modifyTranslationData() {
    performProjectAuthPost(
      "/translations",
      mapOf(
        "key" to "key",
        "translations" to mapOf("en" to UUID.randomUUID().toString()),
      ),
    ).andIsOk
  }
}
