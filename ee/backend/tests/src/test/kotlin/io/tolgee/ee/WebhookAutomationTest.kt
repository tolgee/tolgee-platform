package io.tolgee.ee

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.WebhooksTestData
import io.tolgee.ee.service.WebhookConfigService
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.fixtures.verifyWebhookSignatureHeader
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.util.addSeconds
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.RestTemplate
import java.util.UUID

@SpringBootTest
class WebhookAutomationTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  lateinit var webhookConfigService: WebhookConfigService

  @MockitoBean
  @Autowired
  lateinit var restTemplate: RestTemplate

  @MockitoBean
  @Autowired
  @Qualifier("webhookRestTemplate")
  lateinit var webhookRestTemplate: RestTemplate

  @BeforeEach
  fun before() {
    Mockito.reset(restTemplate, webhookRestTemplate)
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

    verifyWebhookExecuted(testData) {
      modifyTranslationData()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it updates webhook config when failing`() {
    val testData = WebhooksTestData()
    currentDateProvider.forcedDate = currentDateProvider.date
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.projectBuilder.self }
    mockWebhookResponse(HttpStatus.BAD_REQUEST)

    verifyWebhookExecuted(testData) {
      modifyTranslationData()
    }
    webhookConfigService
      .get(testData.webhookConfig.self.id)
      .firstFailed!!
      .time.assert
      .isEqualTo(currentDateProvider.date.time)

    mockWebhookResponse(HttpStatus.OK)

    verifyWebhookExecuted(testData) {
      // webhooks are configured to be retried after 5 seconds
      currentDateProvider.forcedDate = currentDateProvider.date.addSeconds(5)
    }

    verifyWebhookExecuted(testData) {
      modifyTranslationData()
    }

    webhookConfigService
      .get(testData.webhookConfig.self.id)
      .firstFailed.assert
      .isNull()
  }

  private fun mockWebhookResponse(httpStatus: HttpStatus) {
    doAnswer {
      ResponseEntity.status(httpStatus).build<Any>()
    }.whenever(webhookRestTemplate)
      .exchange(
        any<String>(),
        any<HttpMethod>(),
        any<HttpEntity<*>>(),
        any<Class<*>>(),
      )
  }

  private fun getWebhookRestTemplateInvocationCount() = Mockito.mockingDetails(webhookRestTemplate).invocations.count()

  private fun verifyWebhookSignature(
    httpEntity: HttpEntity<String>,
    secret: String,
  ) {
    val signature = httpEntity.headers["Tolgee-Signature"]
    signature.assert.isNotNull
    verifyWebhookSignatureHeader(
      httpEntity.body!!,
      signature!!.single(),
      secret,
      300000,
      currentDateProvider.date.time,
    )
  }

  private fun verifyWebhookExecuted(
    testData: WebhooksTestData,
    webhookTriggeringCallback: () -> Unit,
  ) {
    val invocations = getWebhookRestTemplateInvocationCount()
    webhookTriggeringCallback()
    waitForNotThrowing {
      getWebhookRestTemplateInvocationCount().assert.isEqualTo(invocations + 1)
      val callArguments =
        Mockito
          .mockingDetails(webhookRestTemplate)
          .invocations
          .last()
          .arguments
      callArguments[0]
        .assert
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
      webhookConfigService
        .get(testData.webhookConfig.self.id)
        .lastExecuted!!
        .time.assert
        .isEqualTo(currentDateProvider.date.time)
    }
  }

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
