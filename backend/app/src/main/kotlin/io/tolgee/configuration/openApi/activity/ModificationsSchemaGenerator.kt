package io.tolgee.configuration.openApi.activity

import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.media.Schema
import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.activity.data.EntityDescription
import io.tolgee.activity.data.EntityModificationTypeDefinition
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties

class ModificationsSchemaGenerator {
  fun getModificationSchema(
    entityClass: KClass<*>,
    definition: EntityModificationTypeDefinition<*>,
  ): Schema<*> {
    val schema = getEntitySchema(entityClass)
    schema.required = emptyList()
    val properties = getProperties(entityClass, schema)

    if (definition.isOnlyCreation()) {
      properties.forEach { (_, prop) ->
        prop.properties["old"] = createNullSchema()
      }
    }

    schema.properties = properties

    return schema
  }

  private fun createNullSchema(): Schema<Any> {
    val schema = Schema<Any>()
    schema.type = "null"
    return schema
  }

  private fun EntityModificationTypeDefinition<*>.isOnlyCreation(): Boolean {
    return creation && !deletion && modificationProps.isNullOrEmpty()
  }

  private fun getProperties(
    entityClass: KClass<*>,
    schema: Schema<*>,
  ): Map<String, Schema<*>> {
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

    return singlePropChangeMap + complexPropChangeMap
  }

  private fun getModificationSchemaForComplexProp(it: KClass<*>): Schema<*> {
    val describingProps = it.getDescriptionProps().map { it.name }
    val entitySchema = getEntitySchema(it)
    val schemaDescribingProps =
      entitySchema.properties?.filterKeys { propertyName -> propertyName in describingProps }
    descriptionSchema.properties["data"]?.let { dataProp ->
      dataProp.properties = schemaDescribingProps
      dataProp.additionalProperties = null
    }
    descriptionSchema.additionalProperties = null
    return descriptionSchema.toChangeSchema()
  }

  private fun Schema<*>.toChangeSchema(): Schema<*> {
    val changeSchema = Schema<Any>()
    changeSchema.addProperty("old", this)
    changeSchema.addProperty("new", this)
    return changeSchema
  }

  private fun getEntitySchema(entityClass: KClass<*>): Schema<*> =
    ModelConverters.getInstance()
      .readAllAsResolvedSchema(AnnotatedType(entityClass.java))
      .schema ?: Schema<Any>()

  private fun getAllLoggedProps(entityClass: KClass<*>): List<KProperty1<out Any, *>> {
    return entityClass.memberProperties
      .filter { it.findAnnotation<ActivityLoggedProp>() != null }
      .map { it }
  }

  private fun KClass<*>.getDescriptionProps(): List<KProperty1<out Any, *>> {
    return memberProperties
      .filter { it.findAnnotation<ActivityDescribingProp>() != null }
      .map { it }.filter { it.returnType.classifier.isSimpleType() }
  }

  private fun List<KProperty1<out Any, *>>.getSimpleProps(): List<KProperty1<out Any, *>> {
    return this
      .filter { prop ->
        prop.returnType.classifier.isSimpleType()
      }
  }

  private fun List<KProperty1<out Any, *>>.getComplexProps(): List<KProperty1<out Any, *>> {
    return this
      .filter { prop ->
        !prop.returnType.classifier.isSimpleType()
      }
  }

  private val descriptionSchema by lazy {
    getEntitySchema(EntityDescription::class)
  }

  private fun KClassifier?.isSimpleType(): Boolean {
    return simpleTypes.any { (this as? KClass<*>)?.isSubclassOf(it) == true }
  }

  companion object {
    val simpleTypes =
      setOf(
        Int::class, Long::class, Double::class, Float::class,
        Boolean::class, Char::class, Byte::class, Short::class,
        String::class, Enum::class, Map::class,
      )
  }
}
