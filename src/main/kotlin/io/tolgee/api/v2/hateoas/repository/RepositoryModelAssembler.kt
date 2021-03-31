package io.tolgee.api.v2.hateoas.repository

import io.tolgee.api.v2.hateoas.organization.OrganizationModelAssembler
import io.tolgee.api.v2.hateoas.user_account.UserAccountModelAssembler
import io.tolgee.controllers.RepositoryController
import io.tolgee.model.Repository
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.stereotype.Component

@Component
class RepositoryModelAssembler(
        private val organizationModelAssembler: OrganizationModelAssembler,
        private val userAccountModelAssembler: UserAccountModelAssembler
) : RepresentationModelAssemblerSupport<Repository, RepositoryModel>(
        RepositoryController::class.java, RepositoryModel::class.java) {
    override fun toModel(entity: Repository): RepositoryModel {
        val link = linkTo<RepositoryController> { getRepository(entity.id) }.withSelfRel()

        return RepositoryModel(
                id = entity.id,
                name = entity.name!!,
                description = entity.description,
                addressPart = entity.addressPart!!,
                organizationOwner = entity.organizationOwner?.let { organizationModelAssembler.toModel(it) },
                userOwner = entity.userOwner?.let { userAccountModelAssembler.toModel(it) }
        ).add(link)
    }
}
