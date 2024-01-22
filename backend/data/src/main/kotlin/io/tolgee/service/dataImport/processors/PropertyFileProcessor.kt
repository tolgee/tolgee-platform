package io.tolgee.service.dataImport.processors

import java.util.*

class PropertyFileProcessor(
  override val context: FileProcessorContext,
) : ImportFileProcessor() {
  override fun process() {
    val props = Properties()
    props.load(context.file.data.inputStream())
    props.entries.forEachIndexed { idx, it ->
      context.addTranslation(it.key.toString(), languageNameGuesses[0], it.value, idx)
    }
  }
}
