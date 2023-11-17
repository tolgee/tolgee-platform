package io.tolgee.automation

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.component.contentDelivery.ContentDeliveryFileStorageProvider
import io.tolgee.component.contentDelivery.cachePurging.ContentDeliveryCachePurging
import io.tolgee.component.contentDelivery.cachePurging.ContentDeliveryCachePurgingProvider
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.development.testDataBuilder.data.ContentDeliveryConfigTestData
import io.tolgee.development.testDataBuilder.data.WebhooksTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.fixtures.verifyHeader
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.service.webhooks.WebhookConfigService
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.util.addMinutes
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.AfterEach
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
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
class AutomationIntegrationTest : ProjectAuthControllerTest("/v2/projects/") {

  @MockBean
  @Autowired
  lateinit var contentDeliveryFileStorageProvider: ContentDeliveryFileStorageProvider

  lateinit var fileStorageMock: FileStorage

  @MockBean
  @Autowired
  lateinit var contentDeliveryCachePurgingProvider: ContentDeliveryCachePurgingProvider

  lateinit var purgingMock: ContentDeliveryCachePurging

  @MockBean
  @Autowired
  lateinit var restTemplate: RestTemplate

  @Autowired
  lateinit var webhookConfigService: WebhookConfigService

  @AfterEach
  fun after() {
    currentDateProvider.forcedDate = null
  }

  var webhookInvocationCount = 0

  @Test
  @ProjectJWTAuthTestMethod
  fun `publishes to Content Delivery`() {
    val testData = ContentDeliveryConfigTestData()
    testDataService.saveTestData(testData.root)
    Mockito.reset(restTemplate)
    userAccount = testData.user
    this.projectSupplier = { testData.projectBuilder.self }
    fileStorageMock = mock()
    doReturn(fileStorageMock).whenever(contentDeliveryFileStorageProvider).getContentStorageWithDefaultClient()
    purgingMock = mock()
    doReturn(purgingMock).whenever(contentDeliveryCachePurgingProvider).defaultPurging
    modifyTranslationData()
    verifyContentDeliveryPublish()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it executes webhook`() {
    val testData = WebhooksTestData()
    currentDateProvider.forcedDate = currentDateProvider.date

    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.projectBuilder.self }
    mockWebhookResponse(HttpStatus.OK)

    modifyTranslationData()

    verifyWebhookExecuted(testData)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it updates config when failing`() {
    val testData = WebhooksTestData()
    currentDateProvider.forcedDate = currentDateProvider.date
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.projectBuilder.self }
    mockWebhookResponse(HttpStatus.BAD_REQUEST)

    modifyTranslationData()

    verifyWebhookExecuted(testData)
    webhookConfigService.get(testData.webhookConfig.self.id).firstFailed!!
      .time.assert.isEqualTo(currentDateProvider.date.time)

    mockWebhookResponse(HttpStatus.OK)

    currentDateProvider.forcedDate = currentDateProvider.date.addMinutes(60)
    modifyTranslationData()
    verifyWebhookExecuted(testData)
    webhookConfigService.get(testData.webhookConfig.self.id).firstFailed.assert.isNull()
  }

  private fun mockWebhookResponse(httpStatus: HttpStatus) {
    doAnswer {
      ResponseEntity.status(httpStatus).build<Any>()
    }.whenever(restTemplate)
      .exchange(
        any<String>(),
        any<HttpMethod>(),
        any<HttpEntity<*>>(),
        any<Class<*>>()
      )
  }

  private fun verifyWebhookExecuted(testData: WebhooksTestData) {
    val newExpectedInvocationsCount = ++webhookInvocationCount
    waitForNotThrowing {
      Mockito.mockingDetails(restTemplate).invocations.count().assert.isEqualTo(newExpectedInvocationsCount)
      val callArguments = Mockito.mockingDetails(restTemplate).invocations.last().arguments
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
      webhookConfigService.get(testData.webhookConfig.self.id)
        .lastExecuted!!.time.assert.isEqualTo(currentDateProvider.date.time)
    }
  }

  private fun verifyWebhookSignature(httpEntity: HttpEntity<String>, secret: String) {
    val signature = httpEntity.headers["Tolgee-Signature"]
    signature.assert.isNotNull
    verifyHeader(httpEntity.body, signature!!.single(), secret, 300, currentDateProvider.date.time / 1000)
  }

  private fun verifyContentDeliveryPublish() {
    waitForNotThrowing {
      verify(fileStorageMock, times(1)).storeFile(any(), any())
      val fileStorageInvocations = Mockito.mockingDetails(fileStorageMock).invocations
      fileStorageInvocations.size.assert.isEqualTo(1)
      val purgingInvocations = Mockito.mockingDetails(fileStorageMock).invocations
      purgingInvocations.size.assert.isEqualTo(1)
    }
  }

  private fun modifyTranslationData() {
    performProjectAuthPost(
      "/translations",
      mapOf(
        "key" to "key",
        "translations" to mapOf("en" to UUID.randomUUID().toString())
      )
    ).andIsOk
  }
}
