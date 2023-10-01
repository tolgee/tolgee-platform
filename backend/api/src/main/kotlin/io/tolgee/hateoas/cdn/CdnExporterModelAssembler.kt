package io.tolgee.hateoas.cdn

import io.tolgee.api.v2.controllers.cdn.CdnExporterController
import io.tolgee.model.cdn.CdnExporter
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class CdnExporterModelAssembler() : RepresentationModelAssemblerSupport<CdnExporter, CdnExporterModel>(
  CdnExporterController::class.java, CdnExporterModel::class.java
) {
  override fun toModel(entity: CdnExporter): CdnExporterModel {
    return CdnExporterModel(entity.id, entity.name, entity.slug)
  }
}
