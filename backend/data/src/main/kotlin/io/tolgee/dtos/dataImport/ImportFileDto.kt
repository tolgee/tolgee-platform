package io.tolgee.dtos.dataImport

data class ImportFileDto(
  /**
   * In case of zip file, this is the whole path
   */
  val name: String = "",
  var data: ByteArray,
)
