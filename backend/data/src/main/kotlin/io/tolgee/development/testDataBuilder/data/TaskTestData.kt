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

  var unrelatedOrg: OrganizationBuilder
  var unrelatedProject: ProjectBuilder
  var unrelatedUser: UserAccountBuilder
  var unrelatedEnglish: LanguageBuilder

  init {
    user.name = "Tasks test user"
    projectUser =
      root.addUserAccount {
        username = "project.user@test.com"
        name = "Project user"
      }

    orgMember =
      root.addUserAccount {
        username = "organization.member@test.com"
        name = "Organization member"
      }

    orgAdmin =
      root.addUserAccount {
        username = "organization.owner@test.com"
        name = "Organization owner"
      }

    projectViewScopeUser =
      root.addUserAccount {
        username = "project.view.scope.user@test.com"
        name = "Project view scope user (en)"
      }

    projectViewRoleUser =
      root.addUserAccount {
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

    unrelatedOrg =
      root.addOrganization {
        name = "Unrelated org"
      }

    unrelatedProject =
      root.addProject {
        name = "Unrelated project"
      }

    unrelatedProject.apply {
      unrelatedEnglish = addEnglish()
    }

    unrelatedProject.self.apply {
      languages = mutableSetOf(unrelatedEnglish.self)
      baseLanguage = unrelatedEnglish.self
    }

    unrelatedUser =
      root.addUserAccount {
        username = "Unrelated user"
        name = "Unrelated user"
      }

    unrelatedProject.apply {
      addPermission {
        user = unrelatedUser.self
        type = ProjectPermissionType.EDIT
      }
    }
  }
}
