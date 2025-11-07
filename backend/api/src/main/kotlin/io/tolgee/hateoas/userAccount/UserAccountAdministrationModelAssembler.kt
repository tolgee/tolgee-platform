package io.tolgee.hateoas.userAccount

import io.tolgee.api.isMfaEnabled
import io.tolgee.api.v2.controllers.V2UserController
import io.tolgee.dtos.queryResults.UserAccountAdministrationView
import io.tolgee.model.UserAccount
import io.tolgee.service.AvatarService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class UserAccountAdministrationModelAssembler(
  private val avatarService: AvatarService,
) : RepresentationModelAssemblerSupport<UserAccountAdministrationView, UserAccountAdministrationModel>(
    V2UserController::class.java,
    UserAccountAdministrationModel::class.java,
  ) {
  override fun toModel(view: UserAccountAdministrationView): UserAccountAdministrationModel {
    val avatar = avatarService.getAvatarLinks(view.avatarHash)

    return UserAccountAdministrationModel(
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
