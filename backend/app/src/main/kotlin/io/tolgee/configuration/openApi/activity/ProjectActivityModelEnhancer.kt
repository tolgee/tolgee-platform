package io.tolgee.configuration.openApi.activity

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import io.tolgee.activity.data.ActivityType
import org.apache.commons.text.CaseUtils
import kotlin.reflect.KClass

class ProjectActivityModelEnhancer(
  private val openApi: OpenAPI,
) {
  private val baseSchema =
    openApi.components.schemas["ProjectActivityModel"]
      ?: throw IllegalStateException("ProjectActivityModel schema not found")

  private val modifiedEntityModel =
    openApi.components.schemas["ModifiedEntityModel"]
      ?: throw IllegalStateException("ModifiedEntityModel schema not found")

  private val baseRequired = baseSchema.required

  private val baseProperties = baseSchema.properties

  private val baseType = baseSchema.type

  fun enhance() {
    baseSchema.required = null
    baseSchema.properties = null
    baseSchema.type = null
    baseSchema.oneOf = generateSchemas()
  }

  private fun generateSchemas(): MutableList<Schema<Any>> {
    return ActivityType.entries.map {
      val schemaName = it.getSchemaName()
      Schema<Any>().apply {
        name = schemaName
        properties = getPropertiesForActivitySchema(it)
        type = baseType
        required = baseRequired
      }
    }.toMutableList().also { schemas ->
      openApi.components.schemas.putAll(schemas.associateBy { it.name })
    }
  }

  private fun getPropertiesForActivitySchema(activityType: ActivityType): MutableMap<String, Schema<Any>> {
    val newProperties = baseProperties.toMutableMap()
    newProperties["type"] = getNewTypeProperty(newProperties, activityType)
    newProperties["modifiedEntities"] = getNewModifiedEntitiesProperty(activityType)
    return newProperties
  }

  private fun getNewModifiedEntitiesProperty(activityType: ActivityType): Schema<Any> {
    val properties =
      activityType.typeDefinitions?.map { (entityClass, definition) ->
        val schema = activityType.createModifiedEntityModel(entityClass)
        schema.properties["modifications"] = ModificationsSchemaGenerator(openApi).getModificationSchema(entityClass, definition)
        entityClass.simpleName to schema
      }?.toMap()

    return Schema<Any>().apply {
      name = activityType.getModifiedEntitiesSchemaName()
      this.properties = properties
    }
  }

  private fun getNewTypeProperty(
    properties: Map<String, Schema<*>?>,
    activityType: ActivityType,
  ): Schema<*> {
    val oldTypeProperty = properties["type"] ?: throw IllegalStateException("Type property not found")
    val newType = oldTypeProperty.clone()
    @Suppress("TYPE_MISMATCH_WARNING")
    newType.enum = newType.enum.filter { it == activityType.name }
    return newType
  }

  fun Schema<*>.clone(): Schema<*> {
    val objectMapper = jacksonObjectMapper()
    return objectMapper.readValue(objectMapper.writeValueAsString(this), Schema::class.java)
  }

  private fun ActivityType.createModifiedEntityModel(entityClass: KClass<*>): Schema<*> {
    return Schema<Any>().apply {
      name = this@createModifiedEntityModel.getModifiedEntitySchemaName(entityClass)
      properties = modifiedEntityModel.properties.toMutableMap()
    }
  }

  private fun ActivityType.getModifiedEntitySchemaName(entityClass: KClass<*>): String {
    return "ModifiedEntity" + CaseUtils.toCamelCase(this.name, true, '_') + entityClass.simpleName + "Model"
  }

  private fun ActivityType.getSchemaName() = "ProjectActivity" + CaseUtils.toCamelCase(this.name, true, '_') + "Model"

  private fun ActivityType.getModifiedEntitiesSchemaName() =
    "ModifiedEntities" + CaseUtils.toCamelCase(this.name, true, '_') + "Model"
}
