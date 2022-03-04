package io.tolgee.service.export.exporters

import io.tolgee.dtos.request.export.ExportFormat
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.helpers.TextHelper
import io.tolgee.service.export.dataProvider.ExportTranslationView
import org.json.JSONObject
import java.io.InputStream

class JsonFileExporter(
  override val translations: List<ExportTranslationView>,
  override val exportParams: ExportParams
) : FileExporter {
  override val fileExtension: String = ExportFormat.JSON.extension

  val result = mutableMapOf<String, JSONObject>()

  override fun produceFiles(): Map<String, InputStream> {
    prepare()
    return result.asSequence().map { (fileName, json) -> fileName to json.toString().byteInputStream() }.toMap()
  }

  private fun prepare() {
    translations.forEach { translation ->
      val path = TextHelper.splitOnNonEscapedDelimiter(translation.key.name, exportParams.splitByScopeDelimiter)
      val jsonObject = getJsonObject(path, translation)
      val pathItems = path.asSequence().drop(getRealScopeDepth(path)).toMutableList()
      addToJsonObject(jsonObject, pathItems, translation)
    }
  }

  private fun getJsonObject(path: List<String>, translation: ExportTranslationView): JSONObject {
    val absolutePath = translation.getFileAbsolutePath(path)
    return result[absolutePath] ?: let {
      JSONObject().also { result[absolutePath] = it }
    }
  }

  private fun addToJsonObject(content: JSONObject, pathItems: MutableList<String>, translation: ExportTranslationView) {
    val pathItem = pathItems.removeFirst()
    if (pathItems.size > 0) {
      val jsonObject = JSONObject()
      content.put(pathItem, jsonObject)
      addToJsonObject(jsonObject, pathItems, translation)
      return
    }

    val value = getValueOrJsonNull(translation)
    content.put(pathItem, value)
  }

  /**
   * For some reason putting null into JSONObject deletes the value with specified key, so
   * when we would like to add null, we have to use JSONObject.NULL
   *
   * In cases when we would like to add untranslated values we need to do so.
   */
  private fun getValueOrJsonNull(translation: ExportTranslationView) =
    translation.text ?: let {
      if (exportParams.shouldContainUntranslated) {
        return@let JSONObject.NULL
      }
      return@let null
    }
}
