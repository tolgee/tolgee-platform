package io.tolgee.service.dataImport.processors

import com.fasterxml.jackson.core.JsonParseException
import io.tolgee.exceptions.ImportCannotParseFileException

class PoFileProcessor(
        override val context: FileProcessorContext
) : ImportFileProcessor() {
    override fun process() {
        try {
            context.file.inputStream
        } catch (e: JsonParseException) {
            throw ImportCannotParseFileException(context.file.name, e.message)
        }
    }
}
