package io.tolgee.formats

import io.tolgee.service.dataImport.processors.FileProcessorContext

abstract class ImportFileProcessor {
  abstract val context: FileProcessorContext

  abstract fun process()

  val languageNameGuesses: List<String> by lazy {
    val filePath = context.file.name

    val fileName = filePath.substringAfterLast("/")

    val result =
      arrayOf(
        "^(.*?)\\..*".toRegex(),
        "^(.*?)-.*".toRegex(),
        "^(.*?)_.*".toRegex(),
      ).map {
        fileName.replace(it, "$1")
      }.filter { it.isNotBlank() }

    context.languageNameGuesses = result
    result
  }

  val firstLanguageTagGuessOrUnknown get() = languageNameGuesses.firstOrNull() ?: "unknown"
}
