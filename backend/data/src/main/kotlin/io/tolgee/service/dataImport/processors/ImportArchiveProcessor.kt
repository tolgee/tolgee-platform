package io.tolgee.service.dataImport.processors

import io.tolgee.dtos.dataImport.ImportFileDto

interface ImportArchiveProcessor {
  fun process(file: ImportFileDto): Collection<ImportFileDto>
}
