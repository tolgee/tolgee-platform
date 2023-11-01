package io.tolgee.ee.api.v2.hateoas.cdnStorage

import io.tolgee.ee.api.v2.controllers.CdnStorageController
import io.tolgee.model.cdn.CdnStorage
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class CdnStorageModelAssembler() : RepresentationModelAssemblerSupport<CdnStorage, CdnStorageModel>(
  CdnStorageController::class.java, CdnStorageModel::class.java
) {
  override fun toModel(entity: CdnStorage): CdnStorageModel {
    return CdnStorageModel(entity.id, entity.name)
  }
}
