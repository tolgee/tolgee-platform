package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.TaskState
import io.tolgee.model.enums.TaskType
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.task.Task

class ProjectsTestData : BaseTestData() {
  lateinit var project2English: Language
  lateinit var project2Deutsch: Language
  lateinit var project2: Project
  lateinit var userWithTranslatePermission: UserAccount
  lateinit var task: Task

  init {
    root.apply {
      addUserAccount {
        name = "Franta Kocourek"
        username = "to.si@proladite.krkovicku"
        userWithTranslatePermission = this
      }

      projectBuilder.addKey {
        name = "Untranslated key"
      }
      addProject {
        project2 = this
        name = "Project 2"
      }.build buildProject@{

        addPermission {
          user = this@ProjectsTestData.user
          type = ProjectPermissionType.MANAGE
        }

        project2English =
          addLanguage {
            name = "English"
            tag = "en"
            this@buildProject.self.baseLanguage = this
          }.self

        project2Deutsch =
          addLanguage {
            name = "Deutsch"
            tag = "de"
          }.self

        addPermission {
          user = userWithTranslatePermission
          project = project2
          type = ProjectPermissionType.TRANSLATE
          translateLanguages = mutableSetOf(project2Deutsch, project2English)
        }

        task =
          addTask {
            number = 1
            name = "Translate task"
            type = TaskType.TRANSLATE
            state = TaskState.NEW
            project = project2
            language = englishLanguage
            author = userWithTranslatePermission
          }.self

        addKey {
          name = "Untranslated"
        }

        addKey {
          name = "Translated to both"
        }.build keyBuilder@{
          addTranslation {
            key = this@keyBuilder.self
            text = "Translated"
            language = project2English
          }
          addTranslation {
            key = this@keyBuilder.self
            text = "Translated in de"
            language = project2Deutsch
          }
        }
        addKey {
          name = "Machine translated to en"
        }.build keyBuilder@{
          addTranslation {
            key = this@keyBuilder.self
            text = "Translated"
            state = TranslationState.TRANSLATED
            auto = true
            language = project2English
          }
        }
        addKey {
          name = "Reviewed in de"
        }.build keyBuilder@{
          addTranslation {
            key = this@keyBuilder.self
            text = "Reviewed"
            state = TranslationState.REVIEWED
            language = project2Deutsch
          }
        }
        addKey {
          name = "Needs review in both"
        }.build keyBuilder@{
          addTranslation {
            key = this@keyBuilder.self
            text = "Needs review in de"
            language = project2Deutsch
          }
          addTranslation {
            key = this@keyBuilder.self
            text = "Needs review"
            language = project2English
          }
        }
      }
    }
  }
}
