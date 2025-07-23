package io.tolgee.service.dataImport

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.request.importKeysResolvable.SingleStepImportResolvableItemRequest

class KeysToFilesManager {
  var files: MutableSet<VirtualFile> = mutableSetOf()
  fun processKeys(keys: List<SingleStepImportResolvableItemRequest>) {
    keys.forEach { key ->
      key.translations.entries.forEach { (language, data) ->
        if (data?.text != null) {
          var virtualFile = files.find { it.language == language && it.namespace == (key.namespace ?: "") }
          if (virtualFile == null) {
            virtualFile = VirtualFile(language, key.namespace ?: "")
            files.add(virtualFile)
          }
          virtualFile.records[key.name] = data.text
        }
      }
    }
  }

  fun getDtos(): List<ImportFileDto> {
    return files.map { file ->
      val content = jacksonObjectMapper().writeValueAsString(file.records)
      ImportFileDto("${file.namespace}/${file.language}.json", content.toByteArray())
    }
  }

  companion object {
    class VirtualFile(val language: String, val namespace: String) {
      var records: MutableMap<String, String> = mutableMapOf()

      override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VirtualFile) return false
        return language == other.language && namespace == other.namespace
      }

      override fun hashCode(): Int {
        return 31 * language.hashCode() + namespace.hashCode()
      }
    }
  }
}
