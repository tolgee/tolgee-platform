package io.tolgee.dtos.dataImport

import io.tolgee.dtos.dataImport.ImportFileDto

data class ImportFileProcessorResult(
        val files: List<ImportFileDto>? = null
)
