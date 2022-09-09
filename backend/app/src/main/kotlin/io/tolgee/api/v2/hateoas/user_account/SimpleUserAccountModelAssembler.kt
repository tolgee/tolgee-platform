package io.tolgee.api.v2.hateoas.user_account

import io.tolgee.model.UserAccount
import io.tolgee.security.controllers.UserController
import io.tolgee.service.AvatarService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class SimpleUserAccountModelAssembler(
  private val avatarService: AvatarService
) : RepresentationModelAssemblerSupport<UserAccount, SimpleUserAccountModel>(
  UserController::class.java, SimpleUserAccountModel::class.java
) {
  override fun toModel(entity: UserAccount): SimpleUserAccountModel {
    val avatar = avatarService.getAvatarLinks(entity.avatarHash)

    return SimpleUserAccountModel(
      id = entity.id,
      username = entity.username,
      name = entity.name,
      avatar = avatar,
    )
  }
}
