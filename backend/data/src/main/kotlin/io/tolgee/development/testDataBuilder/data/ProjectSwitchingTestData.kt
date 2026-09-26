package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.enums.ProjectPermissionType

/**
 * One organization with enough projects for the project switcher to show its
 * search box (more than 10) and a second page (more than 20).
 */
class ProjectSwitchingTestData : BaseTestData("projectSwitchingUser", "Current project") {
  init {
    root.apply {
      (1..21).forEach { number ->
        addProject(userAccountBuilder.defaultOrganizationBuilder.self) {
          name = "Other project ${number.toString().padStart(2, '0')}"
        }.build {
          addPermission {
            user = this@ProjectSwitchingTestData.user
            type = ProjectPermissionType.MANAGE
          }
        }
      }
    }
  }
}
