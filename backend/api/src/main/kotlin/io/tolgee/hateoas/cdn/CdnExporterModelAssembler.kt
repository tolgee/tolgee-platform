package io.tolgee.hateoas.cdn

import io.tolgee.api.v2.controllers.cdn.CdnExporterController
import io.tolgee.ee.api.v2.hateoas.cdnStorage.CdnStorageModelAssembler
import io.tolgee.model.cdn.CdnExporter
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class CdnExporterModelAssembler(
  private val cdnStorageModelAssembler: CdnStorageModelAssembler
) : RepresentationModelAssemblerSupport<CdnExporter, CdnExporterModel>(
  CdnExporterController::class.java, CdnExporterModel::class.java
) {
  override fun toModel(entity: CdnExporter): CdnExporterModel {
    return CdnExporterModel(entity.id, entity.name, entity.slug,
      entity.cdnStorage?.let { cdnStorageModelAssembler.toModel(it) }).also {
      it.copyPropsFrom(entity)
    }
  }
}
