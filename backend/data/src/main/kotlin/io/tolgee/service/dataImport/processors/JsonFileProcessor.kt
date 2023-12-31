package io.tolgee.service.dataImport.processors

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.exceptions.ImportCannotParseFileException

class JsonFileProcessor(
  override val context: FileProcessorContext,
) : ImportFileProcessor() {
  override fun process() {
    try {
      val data = jacksonObjectMapper().readValue<Map<Any?, Any?>>(context.file.data)
      val parsed = data.parse()
      parsed.entries.forEachIndexed { index, it ->
        context.addTranslation(it.key, languageNameGuesses[0], it.value, index)
      }
    } catch (e: JsonParseException) {
      throw ImportCannotParseFileException(context.file.name, e.message ?: "")
    } catch (e: MismatchedInputException) {
      throw ImportCannotParseFileException(context.file.name, e.message ?: "")
    }
  }

  private fun Map<*, *>.parse(): Map<String, Any?> {
    val data = mutableMapOf<String, Any?>()
    this.entries.forEachIndexed { idx, entry ->
      val key = entry.key

      if (key !is String) {
        context.fileEntity.addKeyIsNotStringIssue(key.toString(), idx)
        return@forEachIndexed
      }

      (entry.value as? Map<*, *>)?.let { embedded ->
        embedded.parse().forEach { embeddedEntry ->
          data["$key${context.params.structureDelimiter}${embeddedEntry.key}"] = embeddedEntry.value
        }
        return@forEachIndexed
      }

      val value = entry.value
      data[key] = value
      return@forEachIndexed
    }
    return data
  }
}
