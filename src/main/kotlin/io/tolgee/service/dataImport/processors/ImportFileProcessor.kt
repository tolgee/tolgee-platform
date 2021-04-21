package io.tolgee.service.dataImport.processors

interface ImportFileProcessor {
    val context: FileProcessorContext
    fun process()
}
