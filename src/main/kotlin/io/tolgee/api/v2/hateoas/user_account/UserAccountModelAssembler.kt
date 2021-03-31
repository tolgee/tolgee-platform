package io.tolgee.api.v2.hateoas.user_account

import io.tolgee.model.UserAccount
import io.tolgee.security.controllers.UserController
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class UserAccountModelAssembler : RepresentationModelAssemblerSupport<UserAccount, UserAccountModel>(
        UserController::class.java, UserAccountModel::class.java) {
    override fun toModel(entity: UserAccount): UserAccountModel {
        return UserAccountModel(
                entity.id!!,
                entity.username!!,
                entity.name!!
        )
    }
}
