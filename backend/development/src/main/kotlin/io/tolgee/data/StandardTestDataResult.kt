package io.tolgee.data

data class StandardTestDataResult(
  val projects: List<ProjectModel>,
  val users: List<UserModel>,
  val organizations: List<OrganizationModel>,
  val invitations: List<InvitationModel>,
) {
  data class UserModel(
    val name: String,
    val username: String,
    val id: Long,
  )

  data class ProjectModel(
    val name: String,
    val id: Long,
  )

  data class OrganizationModel(
    val id: Long,
    val slug: String,
    val name: String,
    val glossaries: List<GlossaryModel>,
  )

  data class GlossaryModel(
    val id: Long,
    val name: String,
  )

  data class InvitationModel(
    val projectId: Long?,
    val organizationId: Long?,
    val code: String,
  )
}
