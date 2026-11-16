package io.tolgee.hateoas.translations.comments

import io.tolgee.api.v2.controllers.translation.MentionableUsersController
import io.tolgee.service.AvatarService
import io.tolgee.service.translation.MentionableUser
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class MentionedUserModelAssembler(
  private val avatarService: AvatarService,
) : RepresentationModelAssemblerSupport<MentionableUser, MentionedUserModel>(
    MentionableUsersController::class.java,
    MentionedUserModel::class.java,
  ) {
  override fun toModel(entity: MentionableUser): MentionedUserModel =
    MentionedUserModel(
      id = entity.id,
      name = entity.name ?: "",
      avatar = avatarService.getAvatarLinks(entity.avatarHash),
      inThread = entity.inThread,
    )
}
