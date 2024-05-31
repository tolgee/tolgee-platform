package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.formats.ExportFormat
import io.tolgee.hateoas.exportInfo.ExportFormatModel
import io.tolgee.hateoas.exportInfo.ExportFormatModelAssembler
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/public/export-info",
  ],
)
@Tag(name = "Export info")
class ExportInfoController(
  private val exportFormatModelAssembler: ExportFormatModelAssembler,
) {
  @GetMapping(value = ["/formats"])
  fun get(): CollectionModel<ExportFormatModel> {
    return exportFormatModelAssembler.toCollectionModel(ExportFormat.entries)
  }
}
