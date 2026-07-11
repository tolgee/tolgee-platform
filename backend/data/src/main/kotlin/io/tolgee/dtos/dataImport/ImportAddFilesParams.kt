package io.tolgee.dtos.dataImport

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema

open class ImportAddFilesParams(
  @field:Parameter(
    description = IImportAddFilesParams.STRUCTURE_DELIMITER_DESCRIPTION,
    example = IImportAddFilesParams.STRUCTURE_DELIMITER_EXAMPLE,
  )
  @field:Schema(
    description = IImportAddFilesParams.STRUCTURE_DELIMITER_DESCRIPTION,
    example = IImportAddFilesParams.STRUCTURE_DELIMITER_EXAMPLE,
  )
  override var structureDelimiter: Char? = '.',
  @field:Hidden
  override var storeFilesToFileStorage: Boolean = true,
  @field:Parameter(
    description = IImportAddFilesParams.BRANCH_DESCRIPTION,
    example = IImportAddFilesParams.BRANCH_EXAMPLE,
  )
  @field:Schema(
    description = IImportAddFilesParams.BRANCH_DESCRIPTION,
    example = IImportAddFilesParams.BRANCH_EXAMPLE,
  )
  override var branch: String? = null,
) : IImportAddFilesParams
