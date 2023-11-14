package io.tolgee.automation

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.component.cdn.CdnFileStorageProvider
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.development.testDataBuilder.data.CdnTestData
import io.tolgee.development.testDataBuilder.data.WebhooksTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.fixtures.verifyHeader
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

@SpringBootTest
@AutoConfigureMockMvc
class AutomationIntegrationTest : ProjectAuthControllerTest("/v2/projects/") {

  @MockBean
  @Autowired
  lateinit var cdnClientProvider: CdnFileStorageProvider

  lateinit var fileStorageMock: FileStorage

  @MockBean
  @Autowired
  lateinit var restTemplate: RestTemplate

  @Test
  @ProjectJWTAuthTestMethod
  fun `cdn automation works`() {
    val testData = CdnTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.projectBuilder.self }
    fileStorageMock = mock()
    doReturn(fileStorageMock).whenever(cdnClientProvider).getCdnStorageWithDefaultClient()
    modifyTranslationData()
    verifyCdnPublish()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it executes webhook`() {
    val testData = WebhooksTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.projectBuilder.self }
    doAnswer {
      ResponseEntity.status(HttpStatus.OK).build<Any>()
    }.whenever(restTemplate)
      .exchange(
        any<String>(),
        any<HttpMethod>(),
        any<HttpEntity<*>>(),
        any<Class<*>>()
      )

    modifyTranslationData()

    verifyWebhookExecuted(testData)
  }

  private fun verifyWebhookExecuted(testData: WebhooksTestData) {
    waitForNotThrowing {
      val callArguments = Mockito.mockingDetails(restTemplate).invocations.single().arguments
      callArguments[0].assert
        .isEqualTo(testData.webhookConfig.self.url)
      val httpEntity = callArguments[2] as HttpEntity<String>



      verifyWebhookSignature(httpEntity, testData.webhookConfig.self.webhookSecret)

      assertThatJson(httpEntity.body!!) {
        node("webhookConfigId").isValidId
        node("eventType").isEqualTo("PROJECT_ACTIVITY")
        node("activityData") {
          node("revisionId").isNumber
        }
      }
      }
  }

  private fun verifyWebhookSignature(httpEntity: HttpEntity<String>, secret: String) {
    val signature = httpEntity.headers["Tolgee-Signature"]
    signature.assert.isNotNull
    verifyHeader(httpEntity.body, signature!!.single(), secret, 300, currentDateProvider.date.time / 1000)
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


}
