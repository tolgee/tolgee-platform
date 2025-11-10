package io.tolgee.service.dataImport

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.request.ImportFileMapping
import io.tolgee.dtos.request.importKeysResolvable.ResolvableTranslationResolution
import io.tolgee.dtos.request.importKeysResolvable.SingleStepImportResolvableItemRequest
import io.tolgee.dtos.request.importKeysResolvable.SingleStepImportResolvableTranslationRequest
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.util.nullIfEmpty

class KeysToFilesManager {
  private val filesMap: MutableMap<Pair<String, String>, VirtualFile> = mutableMapOf()

  val files: Set<VirtualFile>
    get() = filesMap.values.toSet()

  fun processKeys(keys: List<SingleStepImportResolvableItemRequest>) {
    keys.forEach { key ->
      key.translations.entries.forEach { (language, data) ->
        if (data?.text != null) {
          val fileKey = language to (key.namespace ?: "")
          val virtualFile =
            filesMap.getOrPut(fileKey) {
              VirtualFile(language, key.namespace ?: "")
            }
          virtualFile.records[key.name] = data
        }
      }
    }
  }

  fun getDtos(): List<ImportFileDto> {
    return files.map { file ->
      ImportFileDto(
        getFileName(file.namespace, file.language),
        file.contentsToByteArray(),
      )
    }
  }

  fun getFileMappings(): List<ImportFileMapping> {
    return files.map {
      ImportFileMapping(
        fileName = getFileName(it.namespace, it.language),
        languageTag = it.language,
        namespace = it.namespace.nullIfEmpty,
        format = ImportFormat.JSON_ICU,
      )
    }
  }

  fun getConflictResolutionMap(): MutableMap<
    String,
    MutableMap<String, MutableMap<String, ResolvableTranslationResolution?>>,
  > {
    val map = mutableMapOf<String, MutableMap<String, MutableMap<String, ResolvableTranslationResolution?>>>()
    files.forEach { file ->
      val namespace = map.getOrPut(file.namespace) { mutableMapOf() }
      val language = namespace.getOrPut(file.language) { mutableMapOf() }
      file.records.entries.forEach { (key, data) ->
        language[key] = data.resolution
      }
    }
    return map
  }

  private fun getFileName(
    namespace: String,
    language: String,
  ): String {
    return "$namespace/$language.json"
  }

  companion object {
    data class VirtualFile(
      val language: String,
      val namespace: String,
    ) {
      var records: MutableMap<String, SingleStepImportResolvableTranslationRequest> = mutableMapOf()

      fun contentsToByteArray(): ByteArray {
        val jsonRecord: Map<String, String> = records.map { (key, value) -> key to value.text }.toMap()
        return jacksonObjectMapper().writeValueAsString(jsonRecord).toByteArray()
      }
    }
  }
}
