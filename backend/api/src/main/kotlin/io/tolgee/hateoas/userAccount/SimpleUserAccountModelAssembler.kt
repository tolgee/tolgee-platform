package io.tolgee.hateoas.userAccount

import io.tolgee.api.v2.controllers.V2UserController
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.model.UserAccount
import io.tolgee.service.AvatarService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class SimpleUserAccountModelAssembler(
  private val avatarService: AvatarService,
) : RepresentationModelAssemblerSupport<UserAccount, SimpleUserAccountModel>(
    V2UserController::class.java,
    SimpleUserAccountModel::class.java,
  ) {
  override fun toModel(entity: UserAccount): SimpleUserAccountModel {
    val avatar = avatarService.getAvatarLinks(entity.avatarHash)

    return SimpleUserAccountModel(
      id = entity.id,
      username = entity.username,
      name = entity.name,
      avatar = avatar,
      deleted = entity.deletedAt != null,
    )
  }

  fun toModel(dto: UserAccountDto): SimpleUserAccountModel {
    val avatar = avatarService.getAvatarLinks(dto.avatarHash)

    return SimpleUserAccountModel(
      id = dto.id,
      username = dto.username,
      name = dto.name,
      avatar = avatar,
      deleted = dto.deleted,
    )
  }
}
