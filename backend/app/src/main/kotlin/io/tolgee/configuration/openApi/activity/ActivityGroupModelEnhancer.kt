package io.tolgee.configuration.openApi.activity

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import io.tolgee.activity.groups.ActivityGroupType
import org.apache.commons.text.CaseUtils

class ActivityGroupModelEnhancer(
  private val openApi: OpenAPI,
) {
  private val baseSchema =
    openApi.components.schemas["ActivityGroupModel"]
      ?: throw IllegalStateException("ProjectActivityModel schema not found")

  private val baseProperties = baseSchema.properties

  private val baseType = baseSchema.type

  private val newRequired = baseSchema.required.toMutableList()

  fun enhance() {
    baseSchema.required = null
    baseSchema.properties = null
    baseSchema.type = null
    baseSchema.oneOf = generateSchemas()
  }

  private fun generateSchemas(): MutableList<Schema<Any>> {
    return ActivityGroupType.entries.map {
      val schemaName = it.getSchemaName()
      Schema<Any>().apply {
        name = schemaName
        properties = getPropertiesForActivitySchema(it)
        type = baseType
        required = newRequired
      }
    }.toMutableList().also { schemas ->
      openApi.components.schemas.putAll(schemas.associateBy { it.name })
    }
  }

  private fun getPropertiesForActivitySchema(type: ActivityGroupType): MutableMap<String, Schema<Any>> {
    val newProperties = baseProperties.toMutableMap()
    newProperties["type"] = getNewTypeProperty(newProperties, type)
//    adjustCountsSchema(newProperties, type)
    adjustDataSchema(type, newProperties)
    return newProperties
  }

//  private fun adjustCountsSchema(
//    newProperties: MutableMap<String, Schema<Any>>,
//    type: ActivityGroupType
//  ) {
//    val countPropertyType =
//      newProperties["counts"]?.additionalProperties as? Schema<*>
//        ?: throw IllegalStateException("Counts property not found")
//    newProperties["counts"] = Schema<Any>().also { schema ->
//      schema.type = "object"
//      schema.properties =
//        type.modifications.filter { it.countInView }.also {
//          if (it.isNotEmpty()) {
//            newRequired.add("counts")
//          }
//        }.associate {
//          val className = it.entityClass.simpleName!!
//          schema.addToRequired(className)
//          className to countPropertyType
//        }
//    }
//  }

  private fun Schema<Any>.addToRequired(className: String) {
    if (required == null) {
      required = mutableListOf(className)
      return
    }
    required.add(className)
  }

  private fun adjustDataSchema(
    type: ActivityGroupType,
    newProperties: MutableMap<String, Schema<*>>,
  ) {
    val dataModel = getDataSchema(type)
    if (dataModel != null) {
      newProperties["data"] = dataModel
    } else {
      newProperties.remove("data")
    }
  }

  private fun getDataSchema(type: ActivityGroupType): Schema<*>? {
    val modelType = type.getProvidingModelTypes()?.first
    return modelType?.let { getEntitySchema(openApi, it) }
  }

  private fun getNewTypeProperty(
    properties: Map<String, Schema<*>?>,
    activityType: ActivityGroupType,
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

  private fun ActivityGroupType.getSchemaName() =
    "ActivityGroup" + CaseUtils.toCamelCase(this.name, true, '_') + "Model"
}
