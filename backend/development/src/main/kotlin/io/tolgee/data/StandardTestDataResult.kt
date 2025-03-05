package io.tolgee.data

data class StandardTestDataResult(
  val projects: List<ProjectModel>,
  val users: List<UserModel>,
  val organizations: List<OrganizationModel>,
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
  )
}
