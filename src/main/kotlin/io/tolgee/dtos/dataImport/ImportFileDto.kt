package io.tolgee.dtos.dataImport

import io.tolgee.model.dataImport.ImportArchive
import java.io.InputStream

data class ImportFileDto (
    val name: String?,
    val inputStream: InputStream,
    val archive: ImportArchive? = null
)
