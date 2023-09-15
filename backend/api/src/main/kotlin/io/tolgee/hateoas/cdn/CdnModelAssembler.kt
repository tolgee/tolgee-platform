package io.tolgee.hateoas.cdn

import io.tolgee.api.v2.controllers.CdnController
import io.tolgee.model.cdn.Cdn
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class CdnModelAssembler() : RepresentationModelAssemblerSupport<Cdn, CdnModel>(
  CdnController::class.java, CdnModel::class.java
) {
  override fun toModel(entity: Cdn): CdnModel {
    return CdnModel(entity.id, entity.name, entity.slug)
  }
}
