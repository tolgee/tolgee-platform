package io.tolgee.service.dataImport.processors

abstract class ImportFileProcessor {
    abstract val context: FileProcessorContext
    abstract fun process()

    val languageNameGuesses: List<String> by lazy {
        val result = mutableListOf<String>()
        val fileName = context.file.name ?: return@lazy result
        val guess = fileName.replace("^(.*?)\\..*".toRegex(), "$1")
        if (guess.isNotBlank()) {
            result.add(guess)
        }
        val guess2 = fileName.replace("^(.*?)-.*".toRegex(), "$1")
        if (guess.isNotBlank()) {
            result.add(guess2)
        }
        val guess3 = fileName.replace("^(.*?)_.*".toRegex(), "$1")
        if (guess.isNotBlank()) {
            result.add(guess3)
        }
        context.languageNameGuesses = result
        result
    }
}
