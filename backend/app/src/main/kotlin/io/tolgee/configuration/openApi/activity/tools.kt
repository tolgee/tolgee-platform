package io.tolgee.configuration.openApi.activity

import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import kotlin.reflect.KClass

fun getEntitySchema(
  openApi: OpenAPI,
  entityClass: KClass<*>,
): Schema<*> {
  val resolved =
    ModelConverters.getInstance()
      .readAllAsResolvedSchema(AnnotatedType(entityClass.java))

  resolved.referencedSchemas.forEach(openApi.components.schemas::putIfAbsent)

  return resolved.schema ?: Schema<Any>()
}
