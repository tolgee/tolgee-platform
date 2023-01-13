package io.tolgee.service.dataImport.processors

import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory

abstract class ImportFileProcessor {
  abstract val context: FileProcessorContext
  abstract fun process()

  val languageNameGuesses: List<String> by lazy {
    val fileName = context.file.name

    val result = arrayOf(
      "^(.*?)\\..*".toRegex(),
      "^(.*?)-.*".toRegex(),
      "^(.*?)_.*".toRegex()
    ).map {
      fileName.replace(it, "$1")
    }.filter { it.isNotBlank() }

    context.languageNameGuesses = result
    result
  }

  open val xmlEventReader: XMLEventReader by lazy {
    val inputFactory: XMLInputFactory = XMLInputFactory.newInstance()
    inputFactory.createXMLEventReader(context.file.inputStream)
  }
}
