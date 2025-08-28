package io.tolgee.hateoas.userAccount

import io.tolgee.api.isMfaEnabled
import io.tolgee.api.v2.controllers.V2UserController
import io.tolgee.dtos.queryResults.UserAccountView
import io.tolgee.model.UserAccount
import io.tolgee.service.AvatarService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class PrivateUserAccountModelAssembler(
  private val avatarService: AvatarService,
) : RepresentationModelAssemblerSupport<UserAccountView, PrivateUserAccountModel>(
    V2UserController::class.java,
    PrivateUserAccountModel::class.java,
  ) {
  override fun toModel(view: UserAccountView): PrivateUserAccountModel {
    val avatar = avatarService.getAvatarLinks(view.avatarHash)

    return PrivateUserAccountModel(
      id = view.id,
      username = view.username,
      name = view.name,
      domain = view.domain,
      emailAwaitingVerification = view.emailAwaitingVerification,
      mfaEnabled = view.isMfaEnabled,
      avatar = avatar,
      accountType = view.accountType ?: UserAccount.AccountType.LOCAL,
      thirdPartyAuthType = view.thirdPartyAuthType,
      globalServerRole = view.role ?: UserAccount.Role.USER,
      deletable = view.isDeletable,
      needsSuperJwtToken = view.needsSuperJwt,
    )
  }
}
