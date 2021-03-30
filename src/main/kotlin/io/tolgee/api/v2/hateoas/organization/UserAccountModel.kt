package io.tolgee.api.v2.hateoas.organization

import org.springframework.hateoas.RepresentationModel

data class UserAccountModel(
        val id: Long,
        val username: String,
        var name: String
): RepresentationModel<UserAccountWithOrganizationRoleModel>()
