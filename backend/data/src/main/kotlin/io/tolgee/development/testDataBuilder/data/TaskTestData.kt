package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.*
import io.tolgee.model.Language
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.TaskType

class TaskTestData : BaseTestData("tasksTestUser", "Project with tasks") {
  var projectUser: UserAccountBuilder
  var orgAdmin: UserAccountBuilder
  var orgMember: UserAccountBuilder
  var projectViewScopeUser: UserAccountBuilder
  var projectViewRoleUser: UserAccountBuilder
  var translateTask: TaskBuilder
  var reviewTask: TaskBuilder
  var relatedProject: ProjectBuilder
  var keysInTask: MutableSet<KeyBuilder> = mutableSetOf()
  var keysOutOfTask: MutableSet<KeyBuilder> = mutableSetOf()
  lateinit var czechLanguage: Language

  var unrelatedOrg = OrganizationBuilder(root)
  var unrelatedProject: ProjectBuilder
  var unrelatedUser: UserAccountBuilder
  var unrelatedEnglish: LanguageBuilder

  init {
    user.name = "Tasks test user"
    projectUser = UserAccountBuilder(root)

    projectUser.self.apply {
      username = "project.user@test.com"
      name = "Project user"
    }
    root.data.userAccounts.add(projectUser)

    orgMember = UserAccountBuilder(root)

    orgMember.self.apply {
      username = "organization.member@test.com"
      name = "Organization member"
    }

    orgAdmin = UserAccountBuilder(root)

    orgAdmin.self.apply {
      username = "organization.owner@test.com"
      name = "Organization owner"
    }

    projectViewScopeUser = UserAccountBuilder(root)

    projectViewScopeUser.self.apply {
      username = "project.view.scope.user@test.com"
      name = "Project view scope user (en)"
    }

    projectViewRoleUser = UserAccountBuilder(root)

    projectViewRoleUser.self.apply {
      username = "project.view.role.user@test.com"
      name = "Project view role user (en)"
    }

    userAccountBuilder.defaultOrganizationBuilder.apply {
      addRole {
        user = orgMember.self
        type = OrganizationRoleType.MEMBER
      }

      addRole {
        user = orgAdmin.self
        type = OrganizationRoleType.OWNER
      }
    }

    projectBuilder.apply {
      relatedProject = this

      addLanguage {
        name = "Czech"
        tag = "cs"
        originalName = "Čeština"
        czechLanguage = this
      }

      addPermission {
        user = projectUser.self
        type = ProjectPermissionType.EDIT
      }

      addPermission {
        user = projectViewScopeUser.self
        scopes = arrayOf(Scope.TRANSLATIONS_VIEW)
        viewLanguages = mutableSetOf(englishLanguage)
      }

      addPermission {
        user = projectViewRoleUser.self
        type = ProjectPermissionType.VIEW
        viewLanguages = mutableSetOf(englishLanguage)
      }

      (0 until 2).forEach {
        keysInTask.add(
          addKey(null, "key $it").apply {
            addTranslation("en", "Translation $it")
            addTranslation("cs", "Překlad $it")
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

      translateTask =
        addTask {
          number = 1
          name = "Translate task"
          type = TaskType.TRANSLATE
          assignees =
            mutableSetOf(
              projectUser.self,
              user,
            )
          project = projectBuilder.self
          language = englishLanguage
          author = projectUser.self
        }

      keysInTask.forEach { it ->
        addTaskKey {
          task = translateTask.self
          key = it.self
        }
      }

      reviewTask =
        addTask {
          number = 2
          name = "Review task"
          type = TaskType.REVIEW
          assignees =
            mutableSetOf(
              orgMember.self,
              user,
            )
          project = projectBuilder.self
          language = czechLanguage
          author = projectUser.self
        }

      keysInTask.forEach { it ->
        addTaskKey {
          task = reviewTask.self
          key = it.self
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
      name = "Unrelated user"
    }

    unrelatedProject.apply {
      addPermission {
        user = unrelatedUser.self
        type = ProjectPermissionType.EDIT
      }
    }

    root.data.userAccounts.add(orgMember)
    root.data.userAccounts.add(orgAdmin)
    root.data.organizations.add(unrelatedOrg)
    root.data.projects.add(unrelatedProject)
    root.data.userAccounts.add(unrelatedUser)
    root.data.userAccounts.add(projectViewScopeUser)
    root.data.userAccounts.add(projectViewRoleUser)
  }
}
