package io.tolgee.formats.json.`in`

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.service.dataImport.processors.FileProcessorContext

class JsonFileProcessor(
  override val context: FileProcessorContext,
) : ImportFileProcessor() {
  val result = mutableMapOf<String, MutableList<String?>>()

  override fun process() {
    try {
      val data = jacksonObjectMapper().readValue<Any?>(context.file.data)
      data.parse("")
      result.entries.forEachIndexed { index, (key, translationTexts) ->
        translationTexts.forEach { text ->
          context.addGenericFormatTranslation(key, languageNameGuesses[0], text, index)
        }
      }
    } catch (e: JsonParseException) {
      throw ImportCannotParseFileException(context.file.name, e.message ?: "")
    } catch (e: MismatchedInputException) {
      throw ImportCannotParseFileException(context.file.name, e.message ?: "")
    }
  }

  private fun Any?.parse(keyPrefix: String) {
    (this as? List<*>)?.let {
      it.parseList(keyPrefix)
      return
    }

    (this as? Map<*, *>)?.let {
      it.parseMap(keyPrefix)
      return
    }

    addToResult(keyPrefix, this?.toString())
    return
  }

  private fun addToResult(
    key: String,
    value: String?,
  ) {
    result.compute(key) { _, v ->
      val list = v ?: mutableListOf()
      list.add(value)
      list
    }
  }

  private fun List<*>.parseList(keyPrefix: String) {
    this.forEachIndexed { idx, it ->
      it.parse("$keyPrefix[$idx]")
    }
  }

  private fun Map<*, *>.parseMap(keyPrefix: String) {
    this.entries.forEachIndexed { idx, entry ->
      val key = entry.key

      if (key !is String) {
        context.fileEntity.addKeyIsNotStringIssue(key.toString(), idx)
        return@forEachIndexed
      }

      val keyPrefixWithDelimiter =
        if (keyPrefix.isNotEmpty()) "$keyPrefix${context.params.structureDelimiter}" else ""
      entry.value.parse("$keyPrefixWithDelimiter$key")
      return@forEachIndexed
    }
  }
}
