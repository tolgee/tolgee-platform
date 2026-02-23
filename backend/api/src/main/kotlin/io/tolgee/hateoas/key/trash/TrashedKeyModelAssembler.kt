package io.tolgee.hateoas.key.trash

import io.tolgee.api.v2.controllers.keys.KeyTrashController
import io.tolgee.service.key.KeySearchResultView
import io.tolgee.util.addDays
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TrashedKeyModelAssembler :
  RepresentationModelAssemblerSupport<KeySearchResultView, TrashedKeyModel>(
    KeyTrashController::class.java,
    TrashedKeyModel::class.java,
  ) {
  override fun toModel(entity: KeySearchResultView): TrashedKeyModel {
    val deletedAt = entity.deletedAt!!
    return TrashedKeyModel(
      id = entity.id,
      name = entity.name,
      namespace = entity.namespace,
      deletedAt = deletedAt,
      permanentDeleteAt = deletedAt.addDays(RETENTION_DAYS),
    )
  }

  companion object {
    private const val RETENTION_DAYS = 7
  }
}
