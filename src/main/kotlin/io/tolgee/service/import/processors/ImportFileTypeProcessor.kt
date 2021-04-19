package io.tolgee.service.import.processors

import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.dataImport.ImportFileProcessorResult
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType

interface ImportFileTypeProcessor {
    fun process(file: ImportFileDto, messageClient: (ImportStreamingProgressMessageType, List<Any>?) -> Unit): ImportFileProcessorResult
}
