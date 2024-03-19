package io.tolgee.formats.properties.`in`

import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.service.dataImport.processors.FileProcessorContext
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.io.FileHandler
import java.util.*

class PropertiesFileProcessor(
  override val context: FileProcessorContext,
) : ImportFileProcessor() {
  override fun process() {
    val config = PropertiesConfiguration()
    val handler = FileHandler(config)
    handler.load(context.file.data.inputStream())
    try {
      config.keys.asSequence().forEachIndexed { idx, key ->
        val value = config.getString(key)
        val comment = config.layout.getCanonicalComment(key, false)
        if (!comment.isNullOrBlank()) {
          context.addKeyDescription(key, comment)
        }
        context.addGenericFormatTranslation(key, languageNameGuesses[0], value, idx)
      }
    } catch (e: Exception) {
      throw ImportCannotParseFileException(context.file.name, e.message)
    }
  }
}
