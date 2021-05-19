package io.tolgee.service.dataImport.processors.xliff

import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.exceptions.UnsupportedXliffVersionException
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.ImportFileProcessor

class XliffFileProcessor(override val context: FileProcessorContext) : ImportFileProcessor() {
    override fun process() {
        try {
            when (version) {
                "1.2" -> Xliff12FileProcessor(context, xmlStreamReader).process()
                else -> throw UnsupportedXliffVersionException(version)
            }
        } catch (e: Exception) {
            throw ImportCannotParseFileException(context.file.name, e.message)
        }
    }

    private val version: String by lazy {
        while (xmlStreamReader.hasNext()) {
            xmlStreamReader.next()
            if (xmlStreamReader.isStartElement && xmlStreamReader.localName.toLowerCase() == "xliff") {
                return@lazy xmlStreamReader.getAttributeValue(null, "version")
            }
        }
        throw ImportCannotParseFileException(context.file.name, "No version information")
    }
}
