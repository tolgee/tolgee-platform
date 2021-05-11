package io.tolgee.service.dataImport.processors.poProcessor

import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.exceptions.PoParserException
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.ImportFileProcessor

class PoFileProcessor(
        override val context: FileProcessorContext
) : ImportFileProcessor() {
    override fun process() {
        try {
            val parsed = PoParser(context)()

        } catch (e: PoParserException) {
            throw ImportCannotParseFileException(context.file.name, e.message)
        }
    }


}

