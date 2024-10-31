package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.KeyBuilder
import io.tolgee.development.testDataBuilder.builders.LanguageBuilder
import io.tolgee.development.testDataBuilder.builders.OrganizationBuilder
import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TaskBuilder
import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder
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
  var projectManageRoleUser: UserAccountBuilder
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

  lateinit var blockedTask: TaskBuilder

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

    projectManageRoleUser =
      root.addUserAccount {
        username = "project.manage.role.user@test.com"
        name = "Project manage role user (en)"
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
        user = projectManageRoleUser.self
        type = ProjectPermissionType.MANAGE
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

  fun addBlockedTask() {
    projectBuilder.apply {
      blockedTask =
        addTask {
          number = 3
          name = "Blocked task"
          type = TaskType.REVIEW
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
          task = blockedTask.self
          key = it.self
        }
      }
    }
  }

  fun createManyOutOfTaskKeys(): List<KeyBuilder> {
    val keys =
      (1 until 200).map {
        projectBuilder.run {
          addKey(null, "key in many $it") {
            addTranslation("en", "Translation in many $it")
          }
        }
      }
    keysOutOfTask.addAll(keys)
    return keys
  }

  fun createManyInTaskKeys(): List<KeyBuilder> {
    val keys =
      (1 until 200).map {
        projectBuilder.run {
          addKey(null, "key out many $it") {
            addTranslation("en", "Translation out many $it")
          }
        }
      }
    keys.forEach {
      projectBuilder.addTaskKey {
        task = translateTask.self
        key = it.self
      }
    }
    return keys
  }
}
