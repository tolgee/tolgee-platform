package io.tolgee.hateoas.cdn

import io.tolgee.api.v2.controllers.cdn.CdnExporterController
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.ee.api.v2.hateoas.cdnStorage.CdnStorageModelAssembler
import io.tolgee.model.cdn.CdnExporter
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class CdnExporterModelAssembler(
  private val cdnStorageModelAssembler: CdnStorageModelAssembler,
  private val tolgeeProperties: TolgeeProperties
) : RepresentationModelAssemblerSupport<CdnExporter, CdnExporterModel>(
  CdnExporterController::class.java, CdnExporterModel::class.java
) {
  override fun toModel(entity: CdnExporter): CdnExporterModel {
    return CdnExporterModel(
      id = entity.id,
      name = entity.name,
      slug = entity.slug,
      storage = entity.cdnStorage?.let { cdnStorageModelAssembler.toModel(it) },
      publicUrl = getPublicUrl(entity)
    ).also {
      it.copyPropsFrom(entity)
    }
  }

  private fun getPublicUrl(entity: CdnExporter): String? {
    if (entity.cdnStorage != null) {
      return entity.cdnStorage?.publicUrlPrefix?.let { it.removeSuffix("/") + "/" + entity.slug }
    }
    return tolgeeProperties.cdn.publicUrlPrefix?.let { it.removeSuffix("/") + "/" + entity.slug }
  }
}
