package io.tolgee.configuration.openApi

import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.RequestBody
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.activity.data.ActivityType
import io.tolgee.model.EntityWithId
import io.tolgee.model.Language
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

/**
 * Adds an OpenAPI 3.1 `webhooks` block to the generated spec. One entry per visible
 * `ActivityType` (excluding `hideInList`). Each entry references a generated
 * `AppWebhookPayload_<TYPE>` schema whose `activityData.modifiedEntities` shape is
 * driven by the type's flags:
 *
 * - `onlyCountsInList = true` → `modifiedEntities` is constrained to an empty object,
 *   with all real signal in `counts` (and optionally `params`).
 * - `restrictEntitiesInList != null` → `modifiedEntities` only allows the listed
 *   entity classes.
 * - Otherwise → `modifiedEntities` allows the entity classes this activity actually
 *   produces (defaults to Translation/Key/Language; per-type override table for
 *   narrow events like `SET_TRANSLATIONS → Translation only`).
 *
 * Per-entity `<EntityClass>Modifications` schemas are reflectively built from
 * `@ActivityLoggedProp` annotations on each `@ActivityLoggedEntity`.
 *
 * The cleaner ([OpenApiUnusedSchemaCleaner]) is taught to walk `openAPI.webhooks` too,
 * otherwise our synthesized schemas would be stripped.
 */
class AppWebhookOpenApiCustomizer {
  /** Entity classes whose `@ActivityLoggedProp` fields drive a `<EntityClass>Modifications` schema. */
  private val activityLoggedEntities: List<KClass<out EntityWithId>> =
    listOf(Translation::class, Key::class, Language::class)

  /**
   * Per-activity override: which entity classes this event can produce. If absent,
   * we fall back to `activityLoggedEntities` (all of them), or to
   * `restrictEntitiesInList` when set on the type.
   */
  private val eventEntities: Map<ActivityType, List<KClass<out EntityWithId>>> =
    mapOf(
      ActivityType.SET_TRANSLATIONS to listOf(Translation::class),
      ActivityType.SET_TRANSLATION_STATE to listOf(Translation::class),
      ActivityType.DISMISS_AUTO_TRANSLATED_STATE to listOf(Translation::class),
      ActivityType.SET_OUTDATED_FLAG to listOf(Translation::class),
      ActivityType.KEY_NAME_EDIT to listOf(Key::class),
      ActivityType.KEY_CHARACTER_LIMIT_EDIT to listOf(Key::class),
      ActivityType.KEY_TAGS_EDIT to listOf(Key::class),
      ActivityType.CREATE_KEY to listOf(Key::class),
      ActivityType.KEY_RESTORE to listOf(Key::class),
      ActivityType.CREATE_LANGUAGE to listOf(Language::class),
      ActivityType.EDIT_LANGUAGE to listOf(Language::class),
    )

  fun apply(openAPI: OpenAPI) {
    ensureComponents(openAPI)

    // Collect the union of entity classes referenced by any visible event so we
    // generate one Modifications schema per entity (lazily).
    val allReferencedEntities: Set<KClass<out EntityWithId>> =
      ActivityType.entries
        .filter { !it.hideInList }
        .flatMap { entitiesFor(it) }
        .toSet()
    allReferencedEntities.forEach { entity ->
      val schemaName = "${entity.simpleName}Modifications"
      if (openAPI.components.schemas[schemaName] == null) {
        openAPI.components.schemas[schemaName] = buildEntityModificationsSchema(entity)
      }
    }

    val webhooks = LinkedHashMap<String, PathItem>()
    ActivityType.entries
      .filter { !it.hideInList }
      .forEach { type ->
        val payloadSchemaName = "AppWebhookPayload_${type.name}"
        openAPI.components.schemas[payloadSchemaName] = buildPayloadSchema(type)
        webhooks[type.name] = buildWebhookPathItem(payloadSchemaName, type)
      }

    openAPI.webhooks = (openAPI.webhooks ?: LinkedHashMap()).apply { putAll(webhooks) }
  }

  private fun ensureComponents(openAPI: OpenAPI) {
    if (openAPI.components == null) {
      openAPI.components =
        io.swagger.v3.oas.models
          .Components()
    }
    if (openAPI.components.schemas == null) {
      openAPI.components.schemas = LinkedHashMap()
    }
  }

  private fun buildWebhookPathItem(
    payloadSchemaName: String,
    type: ActivityType,
  ): PathItem {
    val mediaType =
      MediaType().schema(
        Schema<Any>().`$ref`("#/components/schemas/$payloadSchemaName"),
      )
    val content = Content().addMediaType("application/json", mediaType)
    val requestBody = RequestBody().required(true).content(content)
    val operation =
      Operation()
        .operationId("appWebhook_${type.name}")
        .summary("App webhook for activity type ${type.name}")
        .description(
          "Tolgee POSTs this payload to the URL registered by an app install " +
            "subscribed to the `${type.name}` event.",
        ).requestBody(requestBody)
    return PathItem().post(operation)
  }

  /**
   * Builds the `AppWebhookPayload_<TYPE>` schema. The wrapper (`webhookConfigId`,
   * `eventType`, `activityData`) is inlined rather than referenced via $ref because
   * `WebhookRequest` isn't returned by any controller endpoint and so isn't present
   * in `components.schemas`. `activityData` extends the existing `ProjectActivityModel`
   * schema via `allOf` with a per-event narrowing on `type` and `modifiedEntities`.
   */
  private fun buildPayloadSchema(type: ActivityType): Schema<Any> {
    val activityTypeEnum =
      StringSchema().apply {
        addEnumItem(type.name)
      }

    val activityDataNarrowing =
      ObjectSchema().apply {
        addProperty("type", activityTypeEnum)
        addProperty("modifiedEntities", buildModifiedEntitiesSchema(type))
        type.paramsProvider?.let {
          addProperty(
            "params",
            ObjectSchema().additionalProperties(true),
          )
        }
      }

    val activityDataAllOf =
      ObjectSchema().apply {
        allOf =
          mutableListOf<Schema<*>>(
            Schema<Any>().`$ref`("#/components/schemas/ProjectActivityModel"),
            activityDataNarrowing,
          )
      }

    val eventTypeEnum =
      StringSchema().apply {
        addEnumItem("PROJECT_ACTIVITY")
      }

    return ObjectSchema().apply {
      addProperty(
        "webhookConfigId",
        io.swagger.v3.oas.models.media
          .IntegerSchema()
          .format("int64"),
      )
      addProperty("eventType", eventTypeEnum)
      addProperty("activityData", activityDataAllOf)
    } as Schema<Any>
  }

  private fun buildModifiedEntitiesSchema(type: ActivityType): Schema<*> {
    if (type.onlyCountsInList) {
      return ObjectSchema().apply {
        additionalProperties = false
      }
    }

    val allowedEntities = entitiesFor(type)
    val schema =
      ObjectSchema().apply {
        additionalProperties = false
      }
    allowedEntities.forEach { entity ->
      schema.addProperty(entity.simpleName!!, ArraySchema().items(modifiedEntityItemSchema(entity)))
    }
    return schema
  }

  private fun entitiesFor(type: ActivityType): List<KClass<out EntityWithId>> {
    type.restrictEntitiesInList?.let { return it.toList() }
    return eventEntities[type] ?: activityLoggedEntities
  }

  private fun modifiedEntityItemSchema(entity: KClass<out EntityWithId>): Schema<*> {
    return Schema<Any>().apply {
      allOf =
        mutableListOf<Schema<*>>(
          Schema<Any>().`$ref`("#/components/schemas/ModifiedEntityModel"),
          ObjectSchema().apply {
            addProperty(
              "entityClass",
              StringSchema().apply { addEnumItem(entity.simpleName) },
            )
            addProperty(
              "modifications",
              Schema<Any>().`$ref`("#/components/schemas/${entity.simpleName}Modifications"),
            )
          },
        )
    }
  }

  /**
   * Builds the `<EntityClass>Modifications` schema by reflecting `@ActivityLoggedProp`
   * fields on the entity. Each annotated property becomes an optional
   * `PropertyModification` entry.
   */
  private fun buildEntityModificationsSchema(entity: KClass<out EntityWithId>): Schema<Any> {
    val schema = ObjectSchema()
    val propertyModificationRef = Schema<Any>().`$ref`("#/components/schemas/PropertyModification")
    val annotated =
      entity.declaredMemberProperties
        .filter { it.javaField?.getAnnotation(ActivityLoggedProp::class.java) != null }
    if (annotated.isEmpty() || !entity.java.isAnnotationPresent(ActivityLoggedEntity::class.java)) {
      // Permissive shape for entities without per-property reflection coverage
      // (e.g. Glossary, ContentDeliveryConfig). Lets the cleaner & openapi-typescript
      // resolve refs without forcing us to enumerate every annotated entity.
      schema.additionalProperties = propertyModificationRef
    } else {
      annotated.forEach { prop ->
        schema.addProperty(prop.name, propertyModificationRef)
      }
    }
    return schema as Schema<Any>
  }

  private fun addPropertyModificationSchemaIfAbsent(
    @Suppress("UNUSED_PARAMETER") parent: Schema<*>,
  ) {
    // No-op: ensureComponents and the schema map are managed by `apply()`; the
    // PropertyModification schema is added once below in [registerPropertyModificationSchema].
  }

  fun registerPropertyModificationSchema(openAPI: OpenAPI) {
    if (openAPI.components.schemas["PropertyModification"] != null) return
    val pm =
      ObjectSchema().apply {
        addProperty("old", anyValueSchema())
        addProperty("new", anyValueSchema())
      }
    openAPI.components.schemas["PropertyModification"] = pm
  }

  private fun anyValueSchema(): Schema<*> {
    // 3.1-compatible "any" schema: empty schema accepts anything.
    return Schema<Any>()
  }

  companion object {
    @Suppress("unused")
    private val MODEL_CONVERTERS = ModelConverters.getInstance()
  }
}
