package io.tolgee.webhook

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.activity.PublicParamsProvider
import io.tolgee.activity.data.ActivityType
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AbstractControllerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

/**
 * Spec-shape test: proves the generated OpenAPI spec advertises a `webhooks` object
 * with the right shape per [ActivityType], honoring `onlyCountsInList`,
 * `restrictEntitiesInList`, and `paramsProvider`. Does NOT drive any activity — pure
 * spec inspection.
 *
 * Companion test [WebhookPayloadSchemaContractTest] proves that the wire payload Tolgee
 * actually sends matches the schemas this test verifies are well-formed.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WebhookSpecGenerationTest : AbstractControllerTest() {
  companion object {
    @JvmStatic
    fun visibleActivityTypes(): List<ActivityType> = ActivityType.entries.filter { !it.hideInList }

    @JvmStatic
    fun countsOnlyActivityTypes(): List<ActivityType> = visibleActivityTypes().filter { it.onlyCountsInList }

    @JvmStatic
    fun restrictedActivityTypes(): List<ActivityType> =
      visibleActivityTypes().filter { it.restrictEntitiesInList != null }

    @JvmStatic
    fun paramsProviderActivityTypes(): List<ActivityType> = visibleActivityTypes().filter { it.paramsProvider != null }
  }

  private val jacksonMapper = ObjectMapper()

  private fun loadSpec(): JsonNode {
    val response = performGet("/v3/api-docs/All Internal - for Tolgee Web application").andIsOk.andReturn()
    return jacksonMapper.readTree(response.response.contentAsString)
  }

  @Test
  fun `spec advertises OpenAPI 3 1`() {
    val spec = loadSpec()
    val version = spec.path("openapi").asText()
    assertThat(version).startsWith("3.1.")
  }

  @Test
  fun `spec has a webhooks object`() {
    val spec = loadSpec()
    assertThat(spec.has("webhooks")).isTrue
    val webhooks = spec.path("webhooks")
    assertThat(webhooks.isObject).isTrue
    assertThat(webhooks.size()).isGreaterThan(0)
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("visibleActivityTypes")
  fun `webhook entry exists`(type: ActivityType) {
    val spec = loadSpec()
    val webhooks = spec.path("webhooks")
    assertThat(webhooks.has(type.name))
      .withFailMessage("Spec is missing webhook entry for ActivityType.$type")
      .isTrue
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("visibleActivityTypes")
  fun `webhook entry references a payload schema`(type: ActivityType) {
    val spec = loadSpec()
    val schemaRef =
      spec
        .path("webhooks")
        .path(type.name)
        .path("post")
        .path("requestBody")
        .path("content")
        .path("application/json")
        .path("schema")
        .path("\$ref")
        .asText()
    assertThat(schemaRef)
      .withFailMessage("Webhook for $type has no schema ref")
      .matches(Regex("#/components/schemas/AppWebhookPayload_${type.name}").pattern)
    val schemaName = "AppWebhookPayload_${type.name}"
    assertThat(spec.path("components").path("schemas").has(schemaName))
      .withFailMessage("Expected schema $schemaName in components.schemas")
      .isTrue
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("countsOnlyActivityTypes")
  fun `counts-only events constrain modifiedEntities to empty object`(type: ActivityType) {
    val spec = loadSpec()
    val modifiedEntitiesSchema = locateModifiedEntitiesSchema(spec, type)
    assertThat(modifiedEntitiesSchema.path("type").asText())
      .withFailMessage("$type should have modifiedEntities typed as object")
      .isEqualTo("object")
    // No allowed entity-class properties for counts-only events.
    val properties = modifiedEntitiesSchema.path("properties")
    assertThat(properties.isMissingNode || properties.size() == 0)
      .withFailMessage("$type (onlyCountsInList) should have no modifiedEntities entity-class properties")
      .isTrue
    assertThat(modifiedEntitiesSchema.path("additionalProperties").asBoolean(true))
      .withFailMessage("$type modifiedEntities should disallow additionalProperties")
      .isFalse
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("restrictedActivityTypes")
  fun `restricted events expose only listed entity classes`(type: ActivityType) {
    val spec = loadSpec()
    val modifiedEntitiesSchema = locateModifiedEntitiesSchema(spec, type)
    val allowed = type.restrictEntitiesInList!!.map { it.java.simpleName }.toSet()
    val properties = modifiedEntitiesSchema.path("properties")
    assertThat(properties.isObject).isTrue
    val present = properties.fieldNames().asSequence().toSet()
    assertThat(present)
      .withFailMessage("$type modifiedEntities keys should equal $allowed (got $present)")
      .isEqualTo(allowed)
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("paramsProviderActivityTypes")
  fun `events with paramsProvider expose params schema`(type: ActivityType) {
    val spec = loadSpec()
    val activityDataSchema = locateActivityDataSchema(spec, type)
    val paramsSchema = activityDataSchema.path("properties").path("params")
    assertThat(paramsSchema.isMissingNode)
      .withFailMessage("$type has paramsProvider ${type.paramsProvider} but spec lacks params schema")
      .isFalse
    // Either a direct schema or a $ref — both fine, but it shouldn't be `null` or an empty object.
    val hasRef = paramsSchema.has("\$ref")
    val hasType = paramsSchema.has("type")
    val hasAnyOf = paramsSchema.has("anyOf") || paramsSchema.has("oneOf") || paramsSchema.has("allOf")
    assertThat(hasRef || hasType || hasAnyOf)
      .withFailMessage("$type params schema should be non-empty (got $paramsSchema)")
      .isTrue
  }

  @Test
  fun `per-entity Modifications schema exists for Translation`() {
    assertModificationsSchemaExists("Translation")
  }

  @Test
  fun `per-entity Modifications schema exists for Key`() {
    assertModificationsSchemaExists("Key")
  }

  @Test
  fun `per-entity Modifications schema exists for Language`() {
    assertModificationsSchemaExists("Language")
  }

  private fun assertModificationsSchemaExists(entityClass: String) {
    val spec = loadSpec()
    val schemas = spec.path("components").path("schemas")
    val name = "${entityClass}Modifications"
    assertThat(schemas.has(name))
      .withFailMessage("Missing schema $name in components.schemas")
      .isTrue
    val properties = schemas.path(name).path("properties")
    assertThat(properties.isObject).isTrue
    assertThat(properties.size())
      .withFailMessage("$name should have at least one property derived from @ActivityLoggedProp")
      .isGreaterThan(0)
  }

  /**
   * Looks up the `modifiedEntities` schema for a given event, drilling through the
   * AppWebhookPayload_<TYPE> allOf composition to the schema-level `properties.activityData
   * .allOf[1].properties.modifiedEntities` node.
   */
  private fun locateModifiedEntitiesSchema(
    spec: JsonNode,
    type: ActivityType,
  ): JsonNode {
    val activityData = locateActivityDataSchema(spec, type)
    val modifiedEntities = activityData.path("properties").path("modifiedEntities")
    assertThat(modifiedEntities.isMissingNode)
      .withFailMessage("$type payload schema has no activityData.modifiedEntities")
      .isFalse
    return modifiedEntities
  }

  private fun locateActivityDataSchema(
    spec: JsonNode,
    type: ActivityType,
  ): JsonNode {
    val payloadSchema = spec.path("components").path("schemas").path("AppWebhookPayload_${type.name}")
    assertThat(payloadSchema.isMissingNode)
      .withFailMessage("Missing AppWebhookPayload_${type.name}")
      .isFalse
    // We expect allOf composition; the second member should carry properties.activityData with the per-event narrowing.
    val allOf = payloadSchema.path("allOf")
    val narrowing =
      if (allOf.isArray && allOf.size() >= 2) {
        allOf[1]
      } else {
        payloadSchema
      }
    val activityData = narrowing.path("properties").path("activityData")
    assertThat(activityData.isMissingNode)
      .withFailMessage("$type payload schema is missing activityData property")
      .isFalse
    // activityData is itself allOf-composed (base ProjectActivityModel + per-event narrowing).
    val innerAllOf = activityData.path("allOf")
    return if (innerAllOf.isArray && innerAllOf.size() >= 2) {
      innerAllOf[1]
    } else {
      activityData
    }
  }

  @Suppress("UnusedPrivateMember")
  private fun assertProviderInterfaceUsable() {
    // Sanity check: compile-time reference to the provider interface so refactors break us early.
    val ignored: Class<out PublicParamsProvider>? = null

    @Suppress("UNUSED_VARIABLE")
    val unused = ignored
  }
}
