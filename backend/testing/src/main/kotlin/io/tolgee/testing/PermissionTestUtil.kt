package io.tolgee.testing

import io.tolgee.development.DbPopulatorReal
import io.tolgee.dtos.ProjectPermissionData
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.security.PermissionService
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.ResultActions

typealias LangByTag = (tag: String) -> Long

@Component
class PermissionTestUtil(
  private val test: AuthorizedControllerTest,
  private val applicationContext: ApplicationContext,
) {
  private val organizationRoleService: OrganizationRoleService
    get() = applicationContext.getBean(OrganizationRoleService::class.java)

  private val permissionService: PermissionService
    get() = applicationContext.getBean(PermissionService::class.java)

  private val dbPopulator: DbPopulatorReal
    get() = applicationContext.getBean(DbPopulatorReal::class.java)

  fun performSetPermissions(
    type: String,
    getQueryFn: (langByTag: LangByTag) -> String,
  ): ResultActions {
    return withPermissionsTestData { project, user ->
      val languages = project.languages.toList()
      val langByTag = { tag: String -> languages.find { it.tag == tag }!!.id }
      val query = getQueryFn(langByTag)

      val typeAndQuery =
        if (type.isEmpty()) {
          "?$query"
        } else {
          "/$type?$query"
        }

      test.performAuthPut(
        "/v2/projects/${project.id}/users/${user.id}" +
          "/set-permissions$typeAndQuery",
        null,
      )
    }
  }

  fun checkSetPermissionsWithLanguages(
    type: String,
    getQueryFn: (langByTag: LangByTag) -> String,
    checkFn: (data: ProjectPermissionData, langByTag: LangByTag) -> Unit,
  ) {
    withPermissionsTestData { project, user ->
      val languages = project.languages.toList()
      val langByTag = { tag: String -> languages.find { it.tag == tag }!!.id }
      val query = getQueryFn(langByTag)

      val typeAndQuery =
        if (type.isEmpty()) {
          "?$query"
        } else {
          "/$type?$query"
        }

      test
        .performAuthPut(
          "/v2/projects/${project.id}/users/${user.id}" +
            "/set-permissions$typeAndQuery",
          null,
        ).andIsOk

      checkFn(permissionService.getProjectPermissionData(project.id, user.id), langByTag)
    }
  }

  fun <T> withPermissionsTestData(fn: (project: Project, user: UserAccount) -> T): T {
    val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()

    val project =
      usersAndOrganizations[1]
        .organizationRoles[0]
        .organization!!
        .projects[0]

    val user = dbPopulator.createUserIfNotExists("jirina")
    organizationRoleService.grantMemberRoleToUser(user, project.organizationOwner)

    permissionService.create(Permission(user = user, project = project, type = ProjectPermissionType.VIEW))

    test.loginAsUser(usersAndOrganizations[1].name)
    return fn(project, user)
  }
}
