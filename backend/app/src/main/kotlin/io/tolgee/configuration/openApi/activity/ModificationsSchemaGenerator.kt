package io.tolgee.configuration.openApi.activity

import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.media.Schema
import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.activity.data.EntityDescription
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

class ModificationsSchemaGenerator {
  fun getModificationSchema(entityClass: KClass<*>): Schema<*> {
    val schema = getEntitySchema(entityClass)
    val loggedProps = getAllLoggedProps(entityClass)
    val simplePropNames = loggedProps.getSimpleProps().map { it.name }
    val schemaSimpleProps = schema.properties.filterKeys { it in simplePropNames }

    val singlePropChangeMap =
      schemaSimpleProps.map { (name, prop) ->
        name to prop.toChangeSchema()
      }.toMap()

    val complexProps = loggedProps.getComplexProps()
    val complexPropChangeMap =
      complexProps.map {
        it.name to getModificationSchemaForComplexProp(it.returnType.classifier as KClass<*>)
      }.toMap()

    schema.properties = singlePropChangeMap + complexPropChangeMap

    return schema
  }

  private fun getModificationSchemaForComplexProp(it: KClass<*>): Schema<*> {
    val describingProps = it.getDescriptionProps().map { it.name }
    val entitySchema = getEntitySchema(it)
    val schemaDescribingProps =
      entitySchema.properties.filterKeys { propertyName -> propertyName in describingProps }
    descriptionSchema.properties["data"]?.properties = schemaDescribingProps
    descriptionSchema.additionalProperties = null
    return descriptionSchema.toChangeSchema()
  }

  private fun Schema<*>.toChangeSchema(): Schema<*> {
    val changeSchema = Schema<Any>()
    changeSchema.addProperty("old", this)
    changeSchema.addProperty("new", this)
    changeSchema.nullable = true
    return changeSchema
  }

  private fun getEntitySchema(entityClass: KClass<*>): Schema<*> =
    ModelConverters.getInstance()
      .readAllAsResolvedSchema(AnnotatedType(entityClass.java))
      .schema

  private fun getAllLoggedProps(entityClass: KClass<*>): List<KProperty1<out Any, *>> {
    return entityClass.memberProperties
      .filter { it.findAnnotation<ActivityLoggedProp>() != null }
      .map { it }
  }

  private fun KClass<*>.getDescriptionProps(): List<KProperty1<out Any, *>> {
    return memberProperties
      .filter { it.findAnnotation<ActivityDescribingProp>() != null }
      .map { it }.filter { it.returnType.classifier in simpleTypes }
  }

  private fun List<KProperty1<out Any, *>>.getSimpleProps(): List<KProperty1<out Any, *>> {
    return this
      .filter { prop ->
        simpleTypes.any { it == prop.returnType.classifier }
      }
  }

  private fun List<KProperty1<out Any, *>>.getComplexProps(): List<KProperty1<out Any, *>> {
    return this
      .filter { prop ->
        simpleTypes.none { it == prop.returnType.classifier }
      }
  }

  val descriptionSchema by lazy {
    getEntitySchema(EntityDescription::class)
  }

  companion object {
    val simpleTypes =
      setOf(
        Int::class, Long::class, Double::class, Float::class,
        Boolean::class, Char::class, Byte::class, Short::class,
        String::class, Enum::class,
      )
  }
}
