package io.tolgee.service.dataImport.status

import io.tolgee.exceptions.ErrorResponseBody

data class ImportApplicationStatusItem(
  val status: ImportApplicationStatus,
  val errorStatusCode: Int? = null,
  val errorResponseBody: ErrorResponseBody? = null,
)
