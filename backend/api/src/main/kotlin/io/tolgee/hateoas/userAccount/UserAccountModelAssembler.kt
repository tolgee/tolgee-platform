package io.tolgee.hateoas.userAccount

import io.tolgee.api.isMfaEnabled
import io.tolgee.api.v2.controllers.V2UserController
import io.tolgee.dtos.queryResults.UserAccountView
import io.tolgee.model.UserAccount
import io.tolgee.service.AvatarService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class UserAccountModelAssembler(
  private val avatarService: AvatarService,
) : RepresentationModelAssemblerSupport<UserAccountView, UserAccountModel>(
    V2UserController::class.java,
    UserAccountModel::class.java,
  ) {
  override fun toModel(view: UserAccountView): UserAccountModel {
    val avatar = avatarService.getAvatarLinks(view.avatarHash)

    return UserAccountModel(
      id = view.id,
      username = view.username,
      name = view.name,
      emailAwaitingVerification = view.emailAwaitingVerification,
      avatar = avatar,
      globalServerRole = view.role ?: UserAccount.Role.USER,
      mfaEnabled = view.isMfaEnabled,
      deleted = view.deletedAt != null,
      disabled = view.disabledAt != null,
      lastActivity = view.lastActivity,
    )
  }
}
