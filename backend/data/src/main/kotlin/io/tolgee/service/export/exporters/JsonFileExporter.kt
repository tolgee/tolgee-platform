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
    return result.asSequence().map { (fileName, json) -> fileName to json.toString(2).byteInputStream() }.toMap()
  }

  private fun prepare() {
    translations.forEach { translation ->
      val path = TextHelper.splitOnNonEscapedDelimiter(translation.key.name, exportParams.splitByScopeDelimiter)
      val fileJsonObject = getFileJsonObject(path, translation)
      val pathItems = path.asSequence().drop(getRealScopeDepth(path)).toList()
      addToJsonObject(fileJsonObject, pathItems.toMutableList(), translation)
    }
  }

  private fun getFileJsonObject(path: List<String>, translation: ExportTranslationView): JSONObject {
    val absolutePath = translation.getFileAbsolutePath(path)
    return result[absolutePath] ?: let {
      JSONObject().also { result[absolutePath] = it }
    }
  }

  private fun addToJsonObject(content: JSONObject, pathItems: List<String>, translation: ExportTranslationView) {
    val pathItemsMutable = pathItems.toMutableList()
    val pathItem = pathItemsMutable.removeFirst()
    if (pathItemsMutable.size > 0) {
      val jsonObject = content.opt(pathItem) ?: JSONObject().also {
        content.put(pathItem, it)
      }

      if (jsonObject !is JSONObject) {
        handleExistingStringScopeCollision(pathItems, content, translation)
        return
      }

      addToJsonObject(jsonObject, pathItemsMutable, translation)
      return
    }

    content.putTranslationText(pathItem, translation)
  }

  private fun handleExistingStringScopeCollision(
    pathItems: List<String>,
    content: JSONObject,
    translation: ExportTranslationView
  ) {
    val last2joined = pathItems.takeLast(2).joinToString(exportParams.splitByScopeDelimiter + "")
    val joinedPathItems = pathItems.dropLast(2) + last2joined
    addToJsonObject(content, joinedPathItems, translation)
  }

  private fun JSONObject.putTranslationText(
    key: String,
    translation: ExportTranslationView,
  ) {
    val value = getValueOrJsonNull(translation)

    if (this.opt(key) != null) {
      throw StringScopeCollisionException()
    }

    this.put(key, value)
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
