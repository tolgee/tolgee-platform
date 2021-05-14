package io.tolgee.service.dataImport.processors.xliff

import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.exceptions.UnsupportedXliffVersionException
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.ImportFileProcessor
import javax.xml.parsers.DocumentBuilderFactory

class XliffFileProcessor(override val context: FileProcessorContext) : ImportFileProcessor() {
    override fun process() {
        val builderFactory = DocumentBuilderFactory.newInstance()
        val documentBuilder = builderFactory.newDocumentBuilder()
        val doc = documentBuilder.parse(context.file.inputStream)
        try {
            val version = doc.getElementsByTagName("xliff").item(0).attributes.getNamedItem("version").textContent!!
            when (version) {
                "1.2" -> Xliff12FileProcessor(context, doc).process()
                else -> throw UnsupportedXliffVersionException(version)
            }
        } catch (e: Exception) {
            throw ImportCannotParseFileException(context.file.name, e.message)
        }
    }
}
