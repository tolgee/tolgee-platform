package io.tolgee.service.dataImport.processors

abstract class ImportFileProcessor {
  abstract val context: FileProcessorContext

  abstract fun process()

  val languageNameGuesses: List<String> by lazy {
    val fileName = context.file.name

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
}
