package io.tolgee.api.v2.hateoas.user_account

import io.tolgee.model.UserAccount
import io.tolgee.security.controllers.UserController
import io.tolgee.service.AvatarService
import io.tolgee.service.MfaService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class PrivateUserAccountModelAssembler(
  private val avatarService: AvatarService,
  private val mfaService: MfaService
) : RepresentationModelAssemblerSupport<UserAccount, PrivateUserAccountModel>(
  UserController::class.java, PrivateUserAccountModel::class.java
) {
  override fun toModel(entity: UserAccount): PrivateUserAccountModel {
    val avatar = avatarService.getAvatarLinks(entity.avatarHash)

    return PrivateUserAccountModel(
      id = entity.id,
      username = entity.username,
      name = entity.name,
      emailAwaitingVerification = entity.emailVerification?.newEmail,
      mfaEnabled = mfaService.hasMfaEnabled(entity),
      avatar = avatar,
      accountType = entity.accountType ?: UserAccount.AccountType.LOCAL,
      globalServerRole = entity.role ?: UserAccount.Role.USER
    )
  }
}
