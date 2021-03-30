package io.tolgee.api.v2.hateoas.organization

import io.tolgee.model.OrganizationMemberRole
import io.tolgee.model.UserAccount
import io.tolgee.security.controllers.UserController
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class UserAccountWithOrganizationRoleModelAssembler : RepresentationModelAssemblerSupport<Array<Any>, UserAccountWithOrganizationRoleModel>(
        UserController::class.java, UserAccountWithOrganizationRoleModel::class.java) {
    override fun toModel(data: Array<Any>): UserAccountWithOrganizationRoleModel {
        val userAccount = (data[0] as UserAccount)
        val organizationMemberRole = (data[1] as OrganizationMemberRole)
        return UserAccountWithOrganizationRoleModel(
                id = userAccount.id!!,
                name = userAccount.name!!,
                username = userAccount.username!!,
                organizationRoleType = organizationMemberRole.type!!
        )
    }
}
