package io.tolgee.api.v2.hateoas.invitation

import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.util.*

@Relation(collectionRelation = "invitations", itemRelation = "invitation")
open class ProjectInvitationModel(
  val id: Long,
  val code: String,
  val type: ProjectPermissionType?,
  val scopes: Array<Scope>,
  val permittedLanguageIds: List<Long>?,
  val createdAt: Date,
  val invitedUserName: String?,
  val invitedUserEmail: String?
) : RepresentationModel<ProjectInvitationModel>()
