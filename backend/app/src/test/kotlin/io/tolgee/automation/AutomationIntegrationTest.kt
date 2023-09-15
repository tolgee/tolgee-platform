package io.tolgee.automation

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.component.cdn.StorageClientProvider
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.util.function.Consumer

@SpringBootTest
@AutoConfigureMockMvc
class AutomationIntegrationTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: BaseTestData

  @MockBean
  @Autowired
  lateinit var cdnClientProvider: StorageClientProvider

  lateinit var fileStorageMock: FileStorage

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.projectBuilder.self }
    fileStorageMock = mock()
    doReturn(fileStorageMock).whenever(cdnClientProvider).invoke()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `user can create an cdn automation which works`() {
    val cdnId = createCdn()
    createAutomation(cdnId)
    modifyTranslationData()
    verifyCdnPublish()
  }

  private fun verifyCdnPublish() {
    waitForNotThrowing {
      verify(fileStorageMock, times(1)).storeFile(any(), any())
      val invocations = Mockito.mockingDetails(fileStorageMock).invocations
      invocations.size.assert.isEqualTo(1)
    }
  }

  private fun modifyTranslationData() {
    performProjectAuthPost(
      "/translations",
      mapOf(
        "key" to "key",
        "translations" to mapOf("en" to "text")
      )
    ).andIsOk
  }

  private fun createAutomation(cdnId: Long) {
    performProjectAuthPost(
      "/automations",
      mapOf(
        "triggers" to listOf(mapOf("type" to "TRANSLATION_DATA_MODIFICATION")),
        "actions" to listOf(
          mapOf(
            "type" to "CDN_PUBLISH",
            "cdnPublishParams" to mapOf(
              "cdnId" to cdnId
            )
          )
        )
      )
    ).andIsOk
  }

  private fun createCdn(): Long {
    var cdnId = 0L
    performProjectAuthPost("/cdns", mapOf("name" to "Cool CDN", "exportParams" to mapOf("format" to "XLIFF")))
      .andIsOk.andAssertThatJson {
        node("id").isNumber.satisfies(
          Consumer {
            cdnId = it.toLong()
          }
        )
      }
    cdnId.assert.isNotEqualTo(0L)
    return cdnId
  }
}
