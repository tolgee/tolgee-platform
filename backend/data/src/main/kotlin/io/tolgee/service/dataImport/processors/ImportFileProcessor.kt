package io.tolgee.service.dataImport.processors

import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory

abstract class ImportFileProcessor {
  abstract val context: FileProcessorContext
  abstract fun process()

  val languageNameGuesses: List<String> by lazy {
    val result = mutableListOf<String>()
    val fileName = context.file.name
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

  open val xmlEventReader: XMLEventReader by lazy {
    val inputFactory: XMLInputFactory = XMLInputFactory.newInstance()
    inputFactory.createXMLEventReader(context.file.inputStream)
  }
}
