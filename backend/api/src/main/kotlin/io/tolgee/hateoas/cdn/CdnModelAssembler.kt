package io.tolgee.hateoas.cdn

import io.tolgee.api.v2.controllers.cdn.CdnController
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.ee.api.v2.hateoas.cdnStorage.CdnStorageModelAssembler
import io.tolgee.model.cdn.Cdn
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class CdnModelAssembler(
  private val cdnStorageModelAssembler: CdnStorageModelAssembler,
  private val tolgeeProperties: TolgeeProperties
) : RepresentationModelAssemblerSupport<Cdn, CdnModel>(
  CdnController::class.java, CdnModel::class.java
) {
  override fun toModel(entity: Cdn): CdnModel {
    return CdnModel(
      id = entity.id,
      name = entity.name,
      slug = entity.slug,
      storage = entity.cdnStorage?.let { cdnStorageModelAssembler.toModel(it) },
      publicUrl = getPublicUrl(entity),
      autoPublish = entity.automationActions.isNotEmpty()
    ).also {
      it.copyPropsFrom(entity)
    }
  }

  private fun getPublicUrl(entity: Cdn): String? {
    if (entity.cdnStorage != null) {
      return entity.cdnStorage?.publicUrlPrefix?.let { it.removeSuffix("/") + "/" + entity.slug }
    }
    return tolgeeProperties.cdn.publicUrlPrefix?.let { it.removeSuffix("/") + "/" + entity.slug }
  }
}
