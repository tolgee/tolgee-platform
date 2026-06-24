package io.tolgee.webhook.contract

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.development.testDataBuilder.data.WebhooksTestData
import io.tolgee.fixtures.waitForNotThrowing
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.web.client.RestTemplate

/**
 * Helper injected into every webhook contract test class. Owns the test-data
 * fixture, mocks the webhook RestTemplate, captures the JSON Tolgee POSTs, and
 * verifies it conforms to the contract declared in the OpenAPI 3.1 webhooks section.
 *
 * Each test class autowires this and calls [install] from its own `@BeforeEach`,
 * then accesses helpers as `fixture.X` — call sites read explicitly.
 *
 * Note: full JSON Schema 2020-12 validation isn't used here because
 * `json-schema-validator` 2.x has an incompatible API and 1.x is missing classes
 * MCP needs. Instead the fixture does targeted structural assertions that catch
 * the same contract violations (event-type discriminator, modifiedEntities key
 * constraint, presence of params for paramsProvider events). [WebhookSpecGenerationTest]
 * separately proves the spec is well-formed.
 */
@Component
class WebhookContractFixture(
  private val testDataService: TestDataService,
  private val mvc: MockMvc,
  @Qualifier("webhookRestTemplate")
  private val webhookRestTemplate: RestTemplate,
) {
  private val mapper = ObjectMapper()

  private var _testData: WebhooksTestData? = null
  val testData: WebhooksTestData
    get() = _testData ?: error("Call install() first")

  /**
   * Saves a fresh [WebhooksTestData] and stubs the webhookRestTemplate to return 200.
   * Returns the data so the test can wire `userAccount` / `projectSupplier`.
   */
  fun install(): WebhooksTestData {
    Mockito.reset(webhookRestTemplate)
    val data = WebhooksTestData()
    testDataService.saveTestData(data.root)
    _testData = data
    mockWebhookResponse(HttpStatus.OK)
    return data
  }

  fun mockWebhookResponse(status: HttpStatus) {
    doAnswer {
      ResponseEntity.status(status).build<Any>()
    }.whenever(webhookRestTemplate)
      .exchange(
        any<String>(),
        any<HttpMethod>(),
        any<HttpEntity<*>>(),
        any<Class<*>>(),
      )
  }

  /**
   * Polls until a webhook with `activityData.type == eventName` arrives, then returns
   * that payload. Robust against tests that produce multiple webhooks (e.g. setup
   * creates a key, then the test triggers a translation set — we want the latter).
   */
  fun waitForWebhookWithType(eventName: String): JsonNode {
    waitForNotThrowing {
      assertThat(findLastWebhookOfType(eventName))
        .withFailMessage(
          "No webhook with activityData.type=$eventName fired. Observed: ${observedTypes()}",
        ).isNotNull
    }
    return findLastWebhookOfType(eventName)!!
  }

  private fun invocationCount(): Int = Mockito.mockingDetails(webhookRestTemplate).invocations.count()

  @Suppress("UNCHECKED_CAST")
  private fun bodyOfInvocation(invocation: org.mockito.invocation.Invocation): JsonNode? {
    val entity = invocation.arguments.getOrNull(2) as? HttpEntity<String> ?: return null
    val body = entity.body ?: return null
    return mapper.readTree(body)
  }

  private fun findLastWebhookOfType(eventName: String): JsonNode? =
    Mockito
      .mockingDetails(webhookRestTemplate)
      .invocations
      .mapNotNull { bodyOfInvocation(it) }
      .lastOrNull { it.path("activityData").path("type").asText() == eventName }

  private fun observedTypes(): List<String> =
    Mockito
      .mockingDetails(webhookRestTemplate)
      .invocations
      .mapNotNull { bodyOfInvocation(it) }
      .map { it.path("activityData").path("type").asText() }

  /**
   * Fetches the live OpenAPI 3.1 spec for the "All Internal" group via MockMvc.
   */
  fun loadSpec(): JsonNode {
    val response =
      mvc
        .perform(get("/v3/api-docs/All Internal - for Tolgee Web application"))
        .andReturn()
    return mapper.readTree(response.response.contentAsString)
  }

  /**
   * Asserts the captured payload conforms to the spec's contract for [eventName]:
   *  - top-level `eventType` is `PROJECT_ACTIVITY`,
   *  - top-level `webhookConfigId` is present and numeric,
   *  - `activityData.type` is the expected enum value,
   *  - `activityData.modifiedEntities` keys are a subset of the spec's declared
   *    entity-class keys (or empty for `onlyCountsInList` events).
   *
   * Per-event extra checks (e.g. specific `modifications` field present) live in
   * the test class itself, not here.
   */
  fun assertConformsTo(
    payload: JsonNode,
    eventName: String,
  ) {
    val spec = loadSpec()
    val schemaName = "AppWebhookPayload_$eventName"
    val payloadSchema =
      spec.path("components").path("schemas").path(schemaName).also {
        assertThat(it.isMissingNode)
          .withFailMessage("Spec is missing $schemaName")
          .isFalse
      }

    assertThat(payload.path("eventType").asText())
      .withFailMessage("eventType should be PROJECT_ACTIVITY, got ${payload.path("eventType")}")
      .isEqualTo("PROJECT_ACTIVITY")
    assertThat(payload.path("webhookConfigId").isNumber)
      .withFailMessage("webhookConfigId should be numeric")
      .isTrue
    assertThat(payload.path("activityData").path("type").asText())
      .withFailMessage("activityData.type should be $eventName")
      .isEqualTo(eventName)

    val allowedKeys = allowedModifiedEntitiesKeys(payloadSchema)
    val actualKeys =
      payload
        .path("activityData")
        .path("modifiedEntities")
        .fieldNames()
        .asSequence()
        .toSet()
    assertThat(actualKeys)
      .withFailMessage("modifiedEntities keys $actualKeys not a subset of spec-declared $allowedKeys")
      .isSubsetOf(allowedKeys)
  }

  /**
   * Walks the AppWebhookPayload_<TYPE> schema and returns the set of entity class
   * keys allowed by `activityData.modifiedEntities`. For counts-only events this
   * is empty.
   */
  private fun allowedModifiedEntitiesKeys(payloadSchema: JsonNode): Set<String> {
    val activityData = payloadSchema.path("properties").path("activityData")
    val narrowing =
      activityData
        .path("allOf")
        .let { if (it.isArray && it.size() >= 2) it[1] else activityData }
    val modifiedEntities = narrowing.path("properties").path("modifiedEntities")
    val properties = modifiedEntities.path("properties")
    if (properties.isMissingNode || !properties.isObject) return emptySet()
    return properties.fieldNames().asSequence().toSet()
  }
}
