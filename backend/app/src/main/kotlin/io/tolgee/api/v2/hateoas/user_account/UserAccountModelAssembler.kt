package io.tolgee.api.v2.hateoas.user_account

import io.tolgee.model.UserAccount
import io.tolgee.security.controllers.UserController
import io.tolgee.service.AvatarService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class UserAccountModelAssembler(
  private val avatarService: AvatarService
) : RepresentationModelAssemblerSupport<UserAccount, UserAccountModel>(
  UserController::class.java, UserAccountModel::class.java
) {
  override fun toModel(entity: UserAccount): UserAccountModel {
    val avatar = avatarService.getAvatarLinks(entity.avatarHash)

    return UserAccountModel(
      id = entity.id,
      username = entity.username,
      name = entity.name,
      emailAwaitingVerification = entity.emailVerification?.newEmail,
      // note: if support for more MFA methods is added, this check should be reworked to account for them.
      mfaEnabled = entity.totpKey?.isNotEmpty() == true,
      avatar = avatar,
      globalServerRole = entity.role ?: UserAccount.Role.USER
    )
  }
}
