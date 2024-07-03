package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.*
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.TaskType

class TaskTestData : BaseTestData("tagsTestUser", "tagsTestProject") {
  var projectUser: UserAccountBuilder
  var orgMember: UserAccountBuilder
  var createdTask: TaskBuilder
  var relatedProject: ProjectBuilder
  var keysInTask: MutableSet<KeyBuilder> = mutableSetOf()
  var translationsInTask: MutableSet<TranslationBuilder> = mutableSetOf()
  var keysOutOfTask: MutableSet<KeyBuilder> = mutableSetOf()

  var unrelatedOrg = OrganizationBuilder(root)
  var unrelatedProject: ProjectBuilder
  var unrelatedUser: UserAccountBuilder
  var unrelatedEnglish: LanguageBuilder

  init {
    projectUser = UserAccountBuilder(root)

    projectUser.self.apply {
      username = "Project user"
    }
    root.data.userAccounts.add(projectUser)

    orgMember = UserAccountBuilder(root)

    orgMember.self.apply {
      username = "Organization member"
    }
    root.data.userAccounts.add(orgMember)

    userAccountBuilder.defaultOrganizationBuilder.apply {
      addRole {
        user = orgMember.self
        type = OrganizationRoleType.MEMBER
      }
    }

    projectBuilder.apply {
      relatedProject = this

      addPermission {
        user = projectUser.self
        type = ProjectPermissionType.EDIT
      }

      (0 until 2).forEach {
        keysInTask.add(
          addKey(null, "key $it").apply {
            translationsInTask.add(
              addTranslation("en", "Translation $it"),
            )
          },
        )
      }

      (2 until 4).forEach {
        keysOutOfTask.add(
          addKey(null, "key $it").apply {
            addTranslation("en", "Translation $it")
          },
        )
      }

      createdTask =
        addTask {
          name = "New task"
          type = TaskType.TRANSLATE
          assignees =
            mutableSetOf(
              projectUser.self,
            )
          project = projectBuilder.self
          language = englishLanguage
        }

      translationsInTask.forEach { it ->
        addTaskKey {
          task = createdTask.self
          translation = it.self
        }
      }
    }

    unrelatedOrg.self.apply {
      name = "Unrelated org"
    }

    unrelatedProject = ProjectBuilder(unrelatedOrg.self, root)

    unrelatedProject.apply {
      unrelatedEnglish = addEnglish()
    }

    unrelatedProject.self.apply {
      name = "Unrelated project"
      languages = mutableSetOf(unrelatedEnglish.self)
      baseLanguage = unrelatedEnglish.self
    }

    unrelatedUser = UserAccountBuilder(root)

    unrelatedUser.self.apply {
      username = "Unrelated user"
    }

    unrelatedProject.apply {
      addPermission {
        user = unrelatedUser.self
        type = ProjectPermissionType.EDIT
      }
    }

    root.data.organizations.add(unrelatedOrg)
    root.data.projects.add(unrelatedProject)
    root.data.userAccounts.add(unrelatedUser)
  }
}
