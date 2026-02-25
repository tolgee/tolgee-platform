package io.tolgee.hateoas.key.trash

import io.tolgee.api.v2.controllers.keys.KeyTrashController
import io.tolgee.hateoas.userAccount.SimpleUserAccountModel
import io.tolgee.service.AvatarService
import io.tolgee.service.key.KeySearchResultView
import io.tolgee.util.addDays
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TrashedKeyModelAssembler(
  private val avatarService: AvatarService,
) : RepresentationModelAssemblerSupport<KeySearchResultView, TrashedKeyModel>(
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
      deletedBy = buildDeletedByModel(entity),
    )
  }

  private fun buildDeletedByModel(entity: KeySearchResultView): SimpleUserAccountModel? {
    return entity.deletedByUserId?.let { userId ->
      SimpleUserAccountModel(
        id = userId,
        username = entity.deletedByUserUsername ?: "",
        name = entity.deletedByUserName,
        avatar = avatarService.getAvatarLinks(entity.deletedByUserAvatarHash),
        deleted = entity.deletedByUserDeleted ?: false,
      )
    }
  }

  companion object {
    private const val RETENTION_DAYS = 7
  }
}
