package io.tolgee.dtos

import io.tolgee.dtos.dataImport.SimpleImportConflictResult

class ImportResult(
  val unresolvedConflicts: List<SimpleImportConflictResult>?,
)
