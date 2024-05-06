package io.tolgee.hateoas.exportInfo

import io.tolgee.formats.ExportFormat
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class ExportFormatModelAssembler : RepresentationModelAssembler<ExportFormat, ExportFormatModel> {
  override fun toModel(format: ExportFormat): ExportFormatModel {
    return ExportFormatModel(
      format = format,
      extension = format.extension,
      mediaType = format.mediaType,
      defaultFileStructureTemplate = format.defaultFileStructureTemplate,
    )
  }
}
