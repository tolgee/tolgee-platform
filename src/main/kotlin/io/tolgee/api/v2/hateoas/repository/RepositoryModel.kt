package io.tolgee.api.v2.hateoas.repository

import io.tolgee.api.v2.hateoas.organization.OrganizationModel
import io.tolgee.api.v2.hateoas.user_account.UserAccountModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "repositories", itemRelation = "repository")
open class RepositoryModel(
      val id: Long,
      val name: String,
      val description: String?,
      val addressPart: String,
      val userOwner: UserAccountModel?,
      val organizationOwner: OrganizationModel?
) : RepresentationModel<RepositoryModel>()
