package io.tolgee.dtos.dataImport

import java.io.InputStream

data class ImportFileDto(
  val name: String = "",
  val inputStream: InputStream,
)
