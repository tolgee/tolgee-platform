package io.tolgee.hateoas.invitation

import io.tolgee.hateoas.permission.PermissionModel
import io.tolgee.model.enums.ProjectPermissionType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.util.*

@Relation(collectionRelation = "invitations", itemRelation = "invitation")
open class ProjectInvitationModel(
  val id: Long,
  val code: String,
  @Deprecated("Use permission object instead")
  val type: ProjectPermissionType?,
  @Deprecated("Use permission object instead")
  val permittedLanguageIds: List<Long>?,
  val createdAt: Date,
  val invitedUserName: String?,
  val invitedUserEmail: String?,
  val permission: PermissionModel,
) : RepresentationModel<ProjectInvitationModel>()
