package io.tolgee.dtos.request

import io.tolgee.dtos.request.export.ExportParams
import javax.validation.Valid
import javax.validation.constraints.NotBlank

class CdnExporterDto {
  @field:NotBlank
  var name: String = ""

  @field:Valid
  var exportParams: ExportParams = ExportParams()
}
