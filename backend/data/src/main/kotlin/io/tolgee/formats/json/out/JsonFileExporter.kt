package io.tolgee.formats.json.out

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.dtos.IExportParams
import io.tolgee.dtos.request.export.ExportFormat
import io.tolgee.helpers.TextHelper
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import io.tolgee.service.export.exporters.StringScopeCollisionException
import java.io.InputStream

class JsonFileExporter(
  override val translations: List<ExportTranslationView>,
  override val exportParams: IExportParams,
) : FileExporter {
  override val fileExtension: String = ExportFormat.JSON.extension

  val result: LinkedHashMap<String, LinkedHashMap<String, Any?>> = LinkedHashMap()

  override fun produceFiles(): Map<String, InputStream> {
    prepare()
    return result.asSequence().map { (fileName, json) ->
      fileName to jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(json).inputStream()
    }.toMap()
  }

  private fun prepare() {
    translations.forEach { translation ->
      val path = TextHelper.splitOnNonEscapedDelimiter(translation.key.name, exportParams.structureDelimiter)
      val fileContentResultMap = getFileContentResultMap(translation)
      addToMap(fileContentResultMap, path, translation)
    }
  }

  private fun getFileContentResultMap(translation: ExportTranslationView): LinkedHashMap<String, Any?> {
    val absolutePath = translation.getFilePath(translation.key.namespace)
    return result[absolutePath] ?: let {
      LinkedHashMap<String, Any?>().also { result[absolutePath] = it }
    }
  }

  private fun addToMap(
    content: LinkedHashMap<String, Any?>,
    pathItems: List<String>,
    translation: ExportTranslationView,
  ) {
    val pathItemsMutable = pathItems.toMutableList()
    val pathItem = pathItemsMutable.removeFirst()
    if (pathItemsMutable.size > 0) {
      val map =
        content[pathItem] ?: LinkedHashMap<String, Any?>().also {
          content[pathItem] = it
        }

      if (map !is Map<*, *>) {
        handleExistingStringScopeCollision(pathItems, content, translation)
        return
      }

      @Suppress("UNCHECKED_CAST")
      addToMap(map as LinkedHashMap<String, Any?>, pathItemsMutable, translation)
      return
    }

    content.putTranslationText(pathItem, translation)
  }

  private fun handleExistingStringScopeCollision(
    pathItems: List<String>,
    content: LinkedHashMap<String, Any?>,
    translation: ExportTranslationView,
  ) {
    val delimiter = exportParams.structureDelimiter.toString()
    val last2joined = pathItems.takeLast(2).joinToString(delimiter)
    val joinedPathItems = pathItems.dropLast(2) + last2joined
    addToMap(content, joinedPathItems, translation)
  }

  private fun LinkedHashMap<String, Any?>.putTranslationText(
    key: String,
    translation: ExportTranslationView,
  ) {
    val value = translation.text

    if (this.containsKey(key)) {
      throw StringScopeCollisionException()
    }

    this[key] = value
  }
}
