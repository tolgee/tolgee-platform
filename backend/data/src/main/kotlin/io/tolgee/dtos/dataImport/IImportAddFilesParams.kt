package io.tolgee.dtos.dataImport

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Parameter
import io.tolgee.model.branching.Branch

interface IImportAddFilesParams {
  var structureDelimiter: Char?

  @get:Hidden
  @set:Hidden
  @get:Parameter(hidden = true)
  var storeFilesToFileStorage: Boolean

  var branch: String?

  companion object {
    const val STRUCTURE_DELIMITER_DESCRIPTION =
      "When importing files in structured formats (e.g., JSON, YAML), " +
        "this field defines the delimiter which will be used in names of imported keys."

    const val STRUCTURE_DELIMITER_EXAMPLE = "."

    const val BRANCH_DESCRIPTION = "Branch to which files will be imported"
    const val BRANCH_EXAMPLE = Branch.DEFAULT_BRANCH_NAME
  }
}
