package io.tolgee.configuration.openApi

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema

class OpenApiUnusedSchemaCleaner(
  private val openApi: OpenAPI,
) {
  private val usedSchemas = mutableSetOf<String>()

  fun clean() {
    walkPathsAndAddAllSchemas()
    recursivelyWalkAllUsedSchemas()
    removeUnused()
  }

  private fun removeUnused() {
    val toRemove =
      openApi.components.schemas.keys
        .filter { !usedSchemas.contains(it) }
    toRemove.forEach {
      openApi.components.schemas.remove(it)
    }
  }

  private fun recursivelyWalkAllUsedSchemas() {
    var new: Set<String> = usedSchemas
    while (new.isNotEmpty()) {
      val used =
        openApi.components.schemas
          .filter { usedSchemas.contains(it.key) }
          .values
      val names = mutableSetOf<String>()
      used.forEach {
        names.addAll(it.getAllNamesRecursively())
      }
      new = names - usedSchemas
      usedSchemas.addAll(new)
    }
  }

  private fun walkPathsAndAddAllSchemas() {
    openApi.paths.forEach { path ->
      path.value.readOperations().forEach { operation ->
        operation.requestBody?.content?.values?.forEach { mediaType ->
          mediaType.schema.addAllNamesRecursively()
        }
        operation.parameters?.forEach {
          it.schema.addAllNamesRecursively()
        }
        operation.responses.values.forEach { response ->
          response.content?.values?.forEach { mediaType ->
            mediaType.schema.addAllNamesRecursively()
          }
        }
      }
    }
  }

  private val Schema<*>.nameFromRef
    get() = this.`$ref`?.replace("#/components/schemas/", "")

  private fun Schema<*>?.addAllNamesRecursively() {
    usedSchemas.addAll(this.getAllNamesRecursively())
  }

  private fun Schema<*>?.getAllNamesRecursively(): Set<String> {
    val names = mutableSetOf<String>()
    this ?: return names
    this.name?.let { names.add(it) }
    this.nameFromRef?.let { names.add(it) }
    this.properties?.values?.forEach { names.addAll(it.getAllNamesRecursively()) }
    this.additionalItems?.let { names.addAll(it.getAllNamesRecursively()) }
    this.oneOf?.forEach { names.addAll(it.getAllNamesRecursively()) }
    this.anyOf?.forEach { names.addAll(it.getAllNamesRecursively()) }
    this.allOf?.forEach { names.addAll(it.getAllNamesRecursively()) }
    (this.additionalProperties as? Schema<*>)?.let { names.addAll(it.getAllNamesRecursively()) }
    names.addAll(this.items.getAllNamesRecursively())
    return names
  }
}
