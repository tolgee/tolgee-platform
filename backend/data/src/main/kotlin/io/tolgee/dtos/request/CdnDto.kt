package io.tolgee.dtos.request

import io.tolgee.dtos.request.export.ExportParams
import javax.validation.Valid
import javax.validation.constraints.NotBlank

class CdnDto {
  @field:NotBlank
  var name: String = ""

  @field:Valid
  var exportParams: ExportParams = ExportParams()
}
