package io.tolgee.automation

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.component.contentDelivery.ContentDeliveryFileStorageProvider
import io.tolgee.component.contentDelivery.cachePurging.ContentDeliveryCachePurging
import io.tolgee.component.contentDelivery.cachePurging.ContentDeliveryCachePurgingProvider
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.ContentDeliveryConfigBranchingTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class ContentDeliveryBranchAutopushTest : ProjectAuthControllerTest("/v2/projects/") {
  @MockitoBean
  @Autowired
  lateinit var contentDeliveryFileStorageProvider: ContentDeliveryFileStorageProvider

  lateinit var fileStorageMock: FileStorage

  @MockitoBean
  @Autowired
  lateinit var contentDeliveryCachePurgingProvider: ContentDeliveryCachePurgingProvider

  lateinit var purgingMock: ContentDeliveryCachePurging

  @MockitoBean
  @Autowired
  lateinit var enabledFeaturesProvider: EnabledFeaturesProvider

  lateinit var testData: ContentDeliveryConfigBranchingTestData

  @BeforeEach
  fun setup() {
    doReturn(arrayOf(Feature.BRANCHING)).whenever(enabledFeaturesProvider).get(org.mockito.kotlin.any())

    currentDateProvider.forcedDate = currentDateProvider.date
    testData = ContentDeliveryConfigBranchingTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.projectBuilder.self }

    fileStorageMock = mock()
    doReturn(fileStorageMock).whenever(contentDeliveryFileStorageProvider).getContentStorageWithDefaultClient()
    purgingMock = mock()
    doReturn(listOf(purgingMock)).whenever(contentDeliveryCachePurgingProvider).purgings

    // wait for initial invocations from test data saving, then clear
    Thread.sleep(1000)
    Mockito.clearInvocations(fileStorageMock)
  }

  @AfterEach
  fun after() {
    currentDateProvider.forcedDate = null
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `publishes CDN only for matching branch on main branch change`() {
    modifyTranslationOnBranch("key", "main")
    waitForStoreFileCalls(1)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `publishes CDN only for matching branch on feature branch change`() {
    modifyTranslationOnBranch("feature-key", "feature")
    waitForStoreFileCalls(1)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `does not publish feature CDN on main branch change`() {
    modifyTranslationOnBranch("key", "main")
    // only main CDN should publish (1 storeFile call)
    waitForStoreFileCalls(1)

    Mockito.clearInvocations(fileStorageMock)

    modifyTranslationOnBranch("feature-key", "feature")
    // only feature CDN should publish (1 storeFile call)
    waitForStoreFileCalls(1)
  }

  private fun modifyTranslationOnBranch(
    keyName: String,
    branchName: String,
  ) {
    performProjectAuthPost(
      "/translations",
      mapOf(
        "key" to keyName,
        "translations" to mapOf("en" to UUID.randomUUID().toString()),
        "branch" to branchName,
      ),
    ).andIsOk
  }

  private fun waitForStoreFileCalls(expectedCount: Int) {
    waitForNotThrowing(timeout = 3000, pollTime = 200) {
      storeFileInvocations.assert.hasSize(expectedCount)
    }
  }

  private val storeFileInvocations
    get() =
      Mockito
        .mockingDetails(fileStorageMock)
        .invocations
        .filter { it.method.name == "storeFile" }
}
