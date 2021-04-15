package io.tolgee.api.v2.hateoas.invitation

import io.tolgee.api.v2.hateoas.organization.OrganizationModel
import io.tolgee.model.enums.OrganizationRoleType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.util.*

@Relation(collectionRelation = "organizationInvitations", itemRelation = "organizationInvitation")
open class OrganizationInvitationModel(
        val id: Long,
        val code: String,
        val type: OrganizationRoleType,
        val createdAt: Date
) : RepresentationModel<OrganizationInvitationModel>()
